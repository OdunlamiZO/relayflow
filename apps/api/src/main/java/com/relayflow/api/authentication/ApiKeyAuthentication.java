package com.relayflow.api.authentication;

import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Authentication token set in the SecurityContext when a valid API key is presented. The principal
 * carries the workspaceId the key belongs to.
 */
public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final UUID workspaceId;

    public ApiKeyAuthentication(UUID workspaceId) {
        super(List.of());
        this.workspaceId = workspaceId;
        setAuthenticated(true);
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return workspaceId.toString();
    }
}
