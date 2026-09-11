package com.relayflow.api.contact.dto;

import com.relayflow.api.channel.domain.ChannelProvider;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ExternalIdentityResponse(
        UUID id,
        UUID workspaceId,
        UUID contactId,
        UUID channelAccountId,
        ChannelProvider provider,
        String externalUserId,
        String externalConversationId,
        String username,
        Map<String, Object> rawProfile,
        Instant createdAt) {}
