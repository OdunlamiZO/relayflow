package com.relayflow.api.workspace.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Returned once when an API key is created. The {@code key} field contains the full plaintext key —
 * it is never stored and cannot be retrieved again.
 */
public record CreateApiKeyResponse(
        UUID id, String name, String keyPrefix, Instant createdAt, Instant expiresAt, String key) {}
