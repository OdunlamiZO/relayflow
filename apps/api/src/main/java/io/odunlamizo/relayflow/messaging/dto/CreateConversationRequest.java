package io.odunlamizo.relayflow.messaging.dto;

// Docs: docs/codebase-components.md#messaging-dto-records

import io.odunlamizo.relayflow.messaging.domain.ConversationStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateConversationRequest(
        @NotNull UUID workspaceId,
        @NotNull UUID contactId,
        @NotNull UUID channelAccountId,
        ConversationStatus status,
        UUID assigneeId) {}
