package com.relayflow.api.authentication.dto;

public record AuthenticatedUserResponse(
        boolean authenticated,
        boolean anonymous,
        String email,
        String displayName,
        String avatarUrl) {}
