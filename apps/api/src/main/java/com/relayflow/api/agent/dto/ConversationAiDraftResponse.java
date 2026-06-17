package com.relayflow.api.agent.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationAiDraftResponse(
        UUID id,
        UUID workspaceId,
        UUID conversationId,
        UUID invocationLogId,
        String proposedReply,
        List<String> suggestedActions,
        Instant createdAt) {}
