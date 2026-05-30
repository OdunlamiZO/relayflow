package com.relayflow.api.messaging.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContactDetailResponse(
        UUID id,
        UUID workspaceId,
        String displayName,
        Instant createdAt,
        List<ExternalIdentityResponse> identities) {}
