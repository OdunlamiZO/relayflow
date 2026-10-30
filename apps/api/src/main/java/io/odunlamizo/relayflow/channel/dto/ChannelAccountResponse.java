package io.odunlamizo.relayflow.channel.dto;

// Docs: docs/codebase-components.md#channel-dto-records

import io.odunlamizo.relayflow.channel.domain.ChannelAccountStatus;
import io.odunlamizo.relayflow.channel.domain.ChannelProvider;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ChannelAccountResponse(
        UUID id,
        UUID workspaceId,
        ChannelProvider provider,
        String name,
        ChannelAccountStatus status,
        Map<String, Object> metadata,
        String webhookUrl,
        Instant createdAt) {}
