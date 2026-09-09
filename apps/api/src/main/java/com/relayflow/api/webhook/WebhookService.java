package com.relayflow.api.webhook;

import com.relayflow.api.security.CredentialEncryptionService;
import com.relayflow.api.webhook.domain.WorkspaceWebhook;
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

    private final WebhookUrlValidator urlValidator;

    public WebhookService(
            WorkspaceWebhookRepository webhookRepository,
            CredentialEncryptionService encryptionService,
            WebhookUrlValidator urlValidator) {
        this.webhookRepository = webhookRepository;
        this.encryptionService = encryptionService;
        this.urlValidator = urlValidator;
    }

    @Transactional(readOnly = true)
    public Optional<WebhookConfigResponse> getWebhook(UUID workspaceId) {
        return webhookRepository.findByWorkspace(workspaceId).map(w -> toResponse(w, null));
    }

    /**
     * Creates or updates the webhook configuration for a workspace. The secret is never touched
     * here on update — creating auto-generates one (returned once via {@code generatedSecret});
     * changing an existing secret is only done through {@link #rotateSecret}.
     */
    @Transactional
    public WebhookConfigResponse saveWebhook(UUID workspaceId, SaveWebhookRequest request) {
        urlValidator.validate(request.url());

        WorkspaceWebhook webhook = webhookRepository.findByWorkspace(workspaceId).orElse(null);
        String generatedSecret = null;

        if (webhook == null) {
            webhook = new WorkspaceWebhook();
            webhook.setWorkspaceId(workspaceId);

            generatedSecret = generateSecret();
            webhook.setSecret(encryptionService.encrypt(generatedSecret));
        }

        webhook.setUrl(request.url());
        webhook.setEnabled(request.enabled());
        webhook.setEvents(
                request.events() == null
                        ? new LinkedHashSet<>()
                        : new LinkedHashSet<>(request.events()));

        webhook = webhookRepository.save(webhook);

        log.info("Webhook saved: workspaceId={}, url={}", workspaceId, webhook.getUrl());

        return toResponse(webhook, generatedSecret);
    }

    @Transactional
    public void deleteWebhook(UUID workspaceId) {
        webhookRepository
                .findByWorkspace(workspaceId)
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
                        .findByWorkspace(workspaceId)
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

    private WebhookConfigResponse toResponse(WorkspaceWebhook w, String generatedSecret) {
        return new WebhookConfigResponse(
                w.getId(),
                w.getWorkspaceId(),
                w.getUrl(),
                w.isEnabled(),
                w.getEvents(),
                w.getCreatedAt(),
                w.getUpdatedAt(),
                generatedSecret);
    }

    private static String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }
}
