package com.relayflow.api.workspace;

import com.relayflow.api.workspace.domain.WorkspacePermission;
import com.relayflow.api.workspace.dto.SaveSecretRequest;
import com.relayflow.api.workspace.dto.SecretResponse;
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
@RequestMapping("/workspaces/{workspaceId}/secrets")
public class SecretController {

    private final SecretService secretService;

    private final WorkspaceAuthorizationService authorizationService;

    public SecretController(
            SecretService secretService, WorkspaceAuthorizationService authorizationService) {
        this.secretService = secretService;
        this.authorizationService = authorizationService;
    }

    /** Lists every secret's name — never its value. */
    @GetMapping
    List<SecretResponse> listSecrets(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.SECRETS_WRITE);

        return secretService.listSecrets(workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SecretResponse createSecret(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody SaveSecretRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.SECRETS_WRITE);

        return secretService.createSecret(workspaceId, request);
    }

    /** Rotates a secret's value (and optionally its name). The old value is not recoverable. */
    @PutMapping("/{secretId}")
    SecretResponse updateSecret(
            @PathVariable UUID workspaceId,
            @PathVariable UUID secretId,
            @Valid @RequestBody SaveSecretRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.SECRETS_WRITE);

        return secretService.updateSecret(workspaceId, secretId, request);
    }

    @DeleteMapping("/{secretId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteSecret(
            @PathVariable UUID workspaceId,
            @PathVariable UUID secretId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.SECRETS_WRITE);

        secretService.deleteSecret(workspaceId, secretId);
    }
}
