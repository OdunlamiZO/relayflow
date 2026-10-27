package io.odunlamizo.relayflow.webhook;

import io.odunlamizo.relayflow.webhook.dto.RotateWebhookSecretResponse;
import io.odunlamizo.relayflow.webhook.dto.SaveWebhookRequest;
import io.odunlamizo.relayflow.webhook.dto.WebhookConfigResponse;
import io.odunlamizo.relayflow.workspace.WorkspaceAuthorizationService;
import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
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

    @GetMapping
    List<WebhookConfigResponse> listWebhooks(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WEBHOOKS_WRITE);

        return webhookService.listWebhooks(workspaceId);
    }

    @PostMapping
    WebhookConfigResponse createWebhook(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody SaveWebhookRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WEBHOOKS_WRITE);

        return webhookService.createWebhook(workspaceId, request);
    }

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
