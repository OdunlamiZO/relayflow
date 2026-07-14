package com.relayflow.api.authentication.dto;

import java.util.UUID;

public record SignupResponse(
        boolean authenticated, UUID userId, String email, String displayName, UUID workspaceId) {}
