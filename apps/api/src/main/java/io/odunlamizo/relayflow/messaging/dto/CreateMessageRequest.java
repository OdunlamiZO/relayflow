package io.odunlamizo.relayflow.messaging.dto;

// Docs: docs/codebase-components.md#messaging-dto-records

import io.odunlamizo.relayflow.messaging.domain.MessageDirection;
import io.odunlamizo.relayflow.messaging.domain.MessageSenderType;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateMessageRequest(
        @NotNull MessageDirection direction,
        @NotNull MessageSenderType senderType,
        String text,
        String providerMessageId,
        Map<String, Object> rawPayload) {}
