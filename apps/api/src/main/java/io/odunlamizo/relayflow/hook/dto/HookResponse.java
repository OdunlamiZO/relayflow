package io.odunlamizo.relayflow.hook.dto;

// Docs: docs/codebase-components.md#hook

import java.time.Instant;
import java.util.UUID;

public record HookResponse(
        UUID id,
        String key,
        String name,
        String description,
        String expression,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {}
