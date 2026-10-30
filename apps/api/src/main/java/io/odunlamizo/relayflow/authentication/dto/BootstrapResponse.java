package io.odunlamizo.relayflow.authentication.dto;

// Docs: docs/codebase-components.md#auth-dto-records

import java.util.UUID;

public record BootstrapResponse(
        boolean authenticated, UUID userId, String email, String displayName, UUID workspaceId) {}
