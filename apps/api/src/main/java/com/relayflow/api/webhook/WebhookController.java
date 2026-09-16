package com.relayflow.api.webhook;

import com.relayflow.api.webhook.dto.RotateWebhookSecretResponse;
import com.relayflow.api.webhook.dto.SaveWebhookRequest;
import com.relayflow.api.webhook.dto.WebhookConfigResponse;
import com.relayflow.api.workspace.WorkspaceAuthorizationService;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/workspaces/{workspaceId}/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    private final WorkspaceAuthorizationService authorizationService;

    public WebhookController(
            WebhookService webhookService, WorkspaceAuthorizationService authorizationService) {
        this.webhookService = webhookService;
        this.authorizationService = authorizationService;
    }

    /** Lists every webhook configured for this workspace. */
    @GetMapping
    List<WebhookConfigResponse> listWebhooks(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WEBHOOKS_WRITE);

        return webhookService.listWebhooks(workspaceId);
    }

    /** Creates a new webhook for this workspace. */
    @PostMapping
    WebhookConfigResponse createWebhook(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody SaveWebhookRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WEBHOOKS_WRITE);

        return webhookService.createWebhook(workspaceId, request);
    }

    /** Updates an existing webhook. */
    @PutMapping("/{webhookId}")
    WebhookConfigResponse updateWebhook(
            @PathVariable UUID workspaceId,
            @PathVariable UUID webhookId,
            @Valid @RequestBody SaveWebhookRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WEBHOOKS_WRITE);

        return webhookService.updateWebhook(workspaceId, webhookId, request);
    }

    /** Deletes a webhook. */
    @DeleteMapping("/{webhookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteWebhook(
            @PathVariable UUID workspaceId,
            @PathVariable UUID webhookId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WEBHOOKS_WRITE);

        webhookService.deleteWebhook(workspaceId, webhookId);
    }

    /**
     * Generates a new HMAC secret for a webhook, stores it encrypted, and returns the plaintext
     * value once. The caller must persist this value — it cannot be retrieved after this response.
     */
    @PostMapping("/{webhookId}/rotate-secret")
    RotateWebhookSecretResponse rotateSecret(
            @PathVariable UUID workspaceId,
            @PathVariable UUID webhookId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WEBHOOKS_WRITE);

        return webhookService.rotateSecret(workspaceId, webhookId);
    }
}
