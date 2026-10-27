package io.odunlamizo.relayflow.messaging.dto;

import io.odunlamizo.relayflow.messaging.domain.ConversationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateConversationRequest(@NotNull ConversationStatus status) {}
