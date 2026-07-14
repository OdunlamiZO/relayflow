package com.relayflow.api.authentication.dto;

import java.util.UUID;

public record AuthenticatedUserResponse(
        boolean authenticated,
        UUID userId,
        String email,
        String displayName,
        String avatarUrl,
        boolean twoFactorRequired,
        String challengeToken) {}
