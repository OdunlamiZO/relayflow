package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID workspaceId,
        UUID conversationId,
        MessageDirection direction,
        MessageSenderType senderType,
        String text,
        String providerMessageId,
        Map<String, Object> rawPayload,
        Instant createdAt) {}
