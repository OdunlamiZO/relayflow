package com.relayflow.api.workspace.dto;

import java.time.Instant;
import java.util.UUID;

/** The value is never included — a secret can be rotated, but never read back. */
public record SecretResponse(UUID id, String name, Instant createdAt, Instant updatedAt) {}
