package io.odunlamizo.relayflow.workspace.dto;

// Docs: docs/codebase-components.md#workspace-dto-records

import java.time.Instant;
import java.util.UUID;

public record SecretResponse(UUID id, String name, Instant createdAt, Instant updatedAt) {}
