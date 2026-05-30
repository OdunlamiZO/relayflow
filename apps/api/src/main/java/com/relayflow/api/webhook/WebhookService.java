package com.relayflow.api.webhook;

import com.relayflow.api.configuration.CredentialEncryptionService;
import com.relayflow.api.webhook.dto.RotateWebhookSecretResponse;
import com.relayflow.api.webhook.dto.SaveWebhookRequest;
import com.relayflow.api.webhook.dto.WebhookConfigResponse;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WorkspaceWebhookRepository webhookRepository;

    private final CredentialEncryptionService encryptionService;

    public WebhookService(
            WorkspaceWebhookRepository webhookRepository,
            CredentialEncryptionService encryptionService) {
        this.webhookRepository = webhookRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public Optional<WebhookConfigResponse> getWebhook(UUID workspaceId) {
        return webhookRepository.findByWorkspaceId(workspaceId).map(this::toResponse);
    }

    /**
     * Creates or updates the webhook configuration for a workspace.
     *
     * <p>When updating an existing webhook, a null {@code secret} in the request leaves the stored
     * secret unchanged. A non-null secret replaces the stored one (re-encrypted). When creating a
     * new webhook, a secret is mandatory and will be rejected with 400 if absent.
     */
    @Transactional
    public WebhookConfigResponse saveWebhook(UUID workspaceId, SaveWebhookRequest request) {
        WorkspaceWebhook webhook = webhookRepository.findByWorkspaceId(workspaceId).orElse(null);

        if (webhook == null) {
            if (request.secret() == null || request.secret().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "secret is required when creating a webhook");
            }

            webhook = new WorkspaceWebhook();
            webhook.setWorkspaceId(workspaceId);
            webhook.setSecret(encryptionService.encrypt(request.secret()));
        } else if (request.secret() != null && !request.secret().isBlank()) {
            webhook.setSecret(encryptionService.encrypt(request.secret()));
        }

        webhook.setUrl(request.url());
        webhook.setEnabled(request.enabled());
        webhook.setEvents(
                request.events() == null
                        ? new LinkedHashSet<>()
                        : new LinkedHashSet<>(request.events()));

        webhook = webhookRepository.save(webhook);

        log.info("Webhook saved: workspaceId={}, url={}", workspaceId, webhook.getUrl());

        return toResponse(webhook);
    }

    @Transactional
    public void deleteWebhook(UUID workspaceId) {
        webhookRepository
                .findByWorkspaceId(workspaceId)
                .ifPresentOrElse(
                        w -> {
                            webhookRepository.delete(w);
                            log.info("Webhook deleted: workspaceId={}", workspaceId);
                        },
                        () -> {
                            throw new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "No webhook configured for this workspace");
                        });
    }

    /**
     * Generates a new 32-byte random HMAC secret, stores it encrypted, and returns the plaintext
     * value once. The secret cannot be retrieved after this call — callers must persist it.
     */
    @Transactional
    public RotateWebhookSecretResponse rotateSecret(UUID workspaceId) {
        WorkspaceWebhook webhook =
                webhookRepository
                        .findByWorkspaceId(workspaceId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "No webhook configured for this workspace"));

        String newSecret = generateSecret();
        webhook.setSecret(encryptionService.encrypt(newSecret));
        webhookRepository.save(webhook);

        log.info("Webhook secret rotated: workspaceId={}", workspaceId);

        return new RotateWebhookSecretResponse(newSecret);
    }

    private WebhookConfigResponse toResponse(WorkspaceWebhook w) {
        return new WebhookConfigResponse(
                w.getId(),
                w.getWorkspaceId(),
                w.getUrl(),
                w.isEnabled(),
                w.getEvents(),
                w.getCreatedAt(),
                w.getUpdatedAt());
    }

    private static String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }
}
