package io.odunlamizo.relayflow.messaging.dto;

// Docs: docs/codebase-components.md#messaging-dto-records

import io.odunlamizo.relayflow.messaging.domain.ConversationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateConversationRequest(@NotNull ConversationStatus status) {}
