package io.odunlamizo.relayflow.messaging.dto;

import io.odunlamizo.relayflow.messaging.domain.MessageDirection;
import io.odunlamizo.relayflow.messaging.domain.MessageSenderType;
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
