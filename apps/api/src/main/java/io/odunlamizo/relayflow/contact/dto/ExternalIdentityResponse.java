package io.odunlamizo.relayflow.contact.dto;

import io.odunlamizo.relayflow.channel.domain.ChannelProvider;
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
