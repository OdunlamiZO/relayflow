package com.relayflow.api.webhook;

import com.relayflow.api.security.CredentialEncryptionService;
import com.relayflow.api.webhook.domain.WorkspaceWebhook;
import com.relayflow.api.webhook.dto.RotateWebhookSecretResponse;
import com.relayflow.api.webhook.dto.SaveWebhookRequest;
import com.relayflow.api.webhook.dto.WebhookConfigResponse;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
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
    public List<WebhookConfigResponse> listWebhooks(UUID workspaceId) {
        return webhookRepository.findByWorkspace(workspaceId).stream()
                .map(webhook -> toResponse(webhook, null))
                .toList();
    }

    @Transactional
    public WebhookConfigResponse createWebhook(UUID workspaceId, SaveWebhookRequest request) {
        urlValidator.validate(request.url());

        String generatedSecret = generateSecret();

        WorkspaceWebhook webhook = new WorkspaceWebhook();
        webhook.setWorkspaceId(workspaceId);
        webhook.setSecret(encryptionService.encrypt(generatedSecret));
        webhook.setUrl(request.url());
        webhook.setEnabled(request.enabled());
        webhook.setEvents(
                request.events() == null
                        ? new LinkedHashSet<>()
                        : new LinkedHashSet<>(request.events()));

        webhook = webhookRepository.save(webhook);

        log.info("Webhook created: workspaceId={}, url={}", workspaceId, webhook.getUrl());

        return toResponse(webhook, generatedSecret);
    }

    @Transactional
    public WebhookConfigResponse updateWebhook(
            UUID workspaceId, UUID webhookId, SaveWebhookRequest request) {
        urlValidator.validate(request.url());

        WorkspaceWebhook webhook = getOrThrow(workspaceId, webhookId);

        webhook.setUrl(request.url());
        webhook.setEnabled(request.enabled());
        webhook.setEvents(
                request.events() == null
                        ? new LinkedHashSet<>()
                        : new LinkedHashSet<>(request.events()));

        webhook = webhookRepository.save(webhook);

        log.info("Webhook updated: workspaceId={}, webhookId={}", workspaceId, webhookId);

        return toResponse(webhook, null);
    }

    @Transactional
    public void deleteWebhook(UUID workspaceId, UUID webhookId) {
        webhookRepository.delete(getOrThrow(workspaceId, webhookId));

        log.info("Webhook deleted: workspaceId={}, webhookId={}", workspaceId, webhookId);
    }

    @Transactional
    public RotateWebhookSecretResponse rotateSecret(UUID workspaceId, UUID webhookId) {
        WorkspaceWebhook webhook = getOrThrow(workspaceId, webhookId);

        String newSecret = generateSecret();
        webhook.setSecret(encryptionService.encrypt(newSecret));
        webhookRepository.save(webhook);

        log.info("Webhook secret rotated: workspaceId={}, webhookId={}", workspaceId, webhookId);

        return new RotateWebhookSecretResponse(newSecret);
    }

    private WorkspaceWebhook getOrThrow(UUID workspaceId, UUID webhookId) {
        return webhookRepository
                .findInWorkspace(webhookId, workspaceId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Webhook not found"));
    }

    private WebhookConfigResponse toResponse(WorkspaceWebhook webhook, String generatedSecret) {
        return new WebhookConfigResponse(
                webhook.getId(),
                webhook.getWorkspaceId(),
                webhook.getUrl(),
                webhook.isEnabled(),
                webhook.getEvents(),
                webhook.getCreatedAt(),
                webhook.getUpdatedAt(),
                generatedSecret);
    }

    private static String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }
}
