package com.relayflow.api.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record ContactResponse(UUID id, UUID workspaceId, String displayName, Instant createdAt) {}
