package com.relayflow.api.hook.dto;

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
