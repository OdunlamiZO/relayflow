package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateMessageRequest(
        @NotNull MessageDirection direction,
        @NotNull MessageSenderType senderType,
        String text,
        String providerMessageId,
        Map<String, Object> rawPayload) {}
