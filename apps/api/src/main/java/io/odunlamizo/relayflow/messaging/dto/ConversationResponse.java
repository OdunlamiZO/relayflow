package io.odunlamizo.relayflow.messaging.dto;

// Docs: docs/codebase-components.md#messaging-dto-records

import io.odunlamizo.relayflow.channel.domain.ChannelProvider;
import io.odunlamizo.relayflow.messaging.domain.ConversationStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID workspaceId,
        UUID contactId,
        String contactDisplayName,
        Map<String, String> contactTags,
        UUID channelAccountId,
        ChannelProvider channelProvider,
        String channelAccountName,
        ConversationStatus status,
        boolean lockedByWorkflow,
        boolean lockedByAiAgent,
        UUID assigneeId,
        Instant lastMessageAt,
        Instant escalatedAt,
        String escalationReason,
        Instant createdAt) {}
