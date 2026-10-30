package io.odunlamizo.relayflow.authentication.dto;

// Docs: docs/codebase-components.md#auth-dto-records

import java.util.UUID;

public record AuthenticatedUserResponse(
        boolean authenticated,
        UUID userId,
        String email,
        String displayName,
        String avatarUrl,
        boolean twoFactorRequired,
        String challengeToken) {}
