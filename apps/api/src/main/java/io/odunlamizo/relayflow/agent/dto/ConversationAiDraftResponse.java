package io.odunlamizo.relayflow.agent.dto;

// Docs: docs/codebase-components.md#ai-agent-dto-records

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConversationAiDraftResponse(
        UUID id,
        UUID workspaceId,
        UUID conversationId,
        UUID invocationLogId,
        String proposedReply,
        List<String> suggestedActions,
        Map<String, String> extractedData,
        Instant createdAt) {}
