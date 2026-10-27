package io.odunlamizo.relayflow.workspace.dto;

import java.time.Instant;
import java.util.UUID;

public record SecretResponse(UUID id, String name, Instant createdAt, Instant updatedAt) {}
