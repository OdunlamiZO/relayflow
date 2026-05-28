package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.ChannelProvider;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ExternalIdentityResponse(
        UUID id,
        UUID workspaceId,
        UUID contactId,
        ChannelProvider provider,
        String externalUserId,
        String externalConversationId,
        String username,
        Map<String, Object> rawProfile,
        Instant createdAt) {}
