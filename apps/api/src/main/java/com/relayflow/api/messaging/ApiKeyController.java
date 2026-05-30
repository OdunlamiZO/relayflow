package com.relayflow.api.messaging;

import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.messaging.domain.WorkspacePermission;
import com.relayflow.api.messaging.dto.ApiKeyResponse;
import com.relayflow.api.messaging.dto.CreateApiKeyRequest;
import com.relayflow.api.messaging.dto.CreateApiKeyResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final WorkspaceAuthorizationService authorizationService;
    private final SecurityUtils securityUtils;

    public ApiKeyController(
            ApiKeyService apiKeyService,
            WorkspaceAuthorizationService authorizationService,
            SecurityUtils securityUtils) {
        this.apiKeyService = apiKeyService;
        this.authorizationService = authorizationService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    List<ApiKeyResponse> listApiKeys(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.API_KEYS_WRITE);

        return apiKeyService.listApiKeys(workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreateApiKeyResponse createApiKey(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateApiKeyRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.API_KEYS_WRITE);
        UUID userId = securityUtils.resolveUserId(authentication);

        return apiKeyService.createApiKey(workspaceId, userId, request);
    }

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revokeApiKey(
            @PathVariable UUID workspaceId,
            @PathVariable UUID keyId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.API_KEYS_WRITE);

        apiKeyService.revokeApiKey(workspaceId, keyId);
    }
}
