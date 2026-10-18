package com.relayflow.api.workspace.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateApiKeyResponse(
        UUID id, String name, String keyPrefix, Instant createdAt, Instant expiresAt, String key) {}
