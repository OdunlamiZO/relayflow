package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.ConversationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateConversationRequest(@NotNull ConversationStatus status) {}
