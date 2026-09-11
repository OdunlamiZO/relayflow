package com.relayflow.api.webhook;

import com.relayflow.api.webhook.dto.RotateWebhookSecretResponse;
import com.relayflow.api.webhook.dto.SaveWebhookRequest;
import com.relayflow.api.webhook.dto.WebhookConfigResponse;
import com.relayflow.api.workspace.WorkspaceAuthorizationService;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/workspaces/{workspaceId}/webhook")
public class WebhookController {

    private final WebhookService webhookService;

    private final WorkspaceAuthorizationService authorizationService;

    public WebhookController(
            WebhookService webhookService, WorkspaceAuthorizationService authorizationService) {
        this.webhookService = webhookService;
        this.authorizationService = authorizationService;
    }

    /** Returns the workspace webhook configuration, or 404 if none is configured. */
    @GetMapping
    ResponseEntity<WebhookConfigResponse> getWebhook(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WEBHOOKS_WRITE);

        return webhookService
                .getWebhook(workspaceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Creates or updates the webhook configuration for this workspace. */
    @PutMapping
    WebhookConfigResponse saveWebhook(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody SaveWebhookRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WEBHOOKS_WRITE);

        return webhookService.saveWebhook(workspaceId, request);
    }

    /** Deletes the webhook configuration. Returns 404 if none was configured. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteWebhook(@PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WEBHOOKS_WRITE);

        webhookService.deleteWebhook(workspaceId);
    }

    /**
     * Generates a new HMAC secret, stores it encrypted, and returns the plaintext value once. The
     * caller must persist this value — it cannot be retrieved after this response.
     */
    @PostMapping("/rotate-secret")
    RotateWebhookSecretResponse rotateSecret(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WEBHOOKS_WRITE);

        return webhookService.rotateSecret(workspaceId);
    }
}
