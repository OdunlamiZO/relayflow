package com.relayflow.api.messaging.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        UUID workspaceId,
        String displayName,
        Map<String, String> customFields,
        Instant createdAt,
        List<ExternalIdentityResponse> identities) {}
