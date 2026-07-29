package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.ChannelProvider;
import com.relayflow.api.messaging.domain.ConversationStatus;
import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID workspaceId,
        UUID contactId,
        String contactDisplayName,
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
