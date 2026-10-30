package io.odunlamizo.relayflow.workspace.dto;

// Docs: docs/codebase-components.md#workspace-dto-records

import java.time.Instant;
import java.util.UUID;

public record CreateApiKeyResponse(
        UUID id, String name, String keyPrefix, Instant createdAt, Instant expiresAt, String key) {}
