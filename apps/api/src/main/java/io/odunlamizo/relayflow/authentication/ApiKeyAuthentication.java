package io.odunlamizo.relayflow.authentication;

// Docs: docs/codebase-components.md#apikeyauthentication

import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

@Getter
public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final UUID workspaceId;

    public ApiKeyAuthentication(UUID workspaceId) {
        super(List.of());
        this.workspaceId = workspaceId;
        setAuthenticated(true);
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
