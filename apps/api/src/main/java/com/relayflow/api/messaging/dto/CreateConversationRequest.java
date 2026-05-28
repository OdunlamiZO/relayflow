package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.ConversationStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateConversationRequest(
        @NotNull UUID workspaceId,
        @NotNull UUID contactId,
        @NotNull UUID channelAccountId,
        ConversationStatus status,
        UUID assignedUserId) {}
