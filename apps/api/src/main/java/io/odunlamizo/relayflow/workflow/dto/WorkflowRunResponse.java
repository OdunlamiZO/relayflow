package io.odunlamizo.relayflow.workflow.dto;

// Docs: docs/codebase-components.md#workflow-dto-records

import io.odunlamizo.relayflow.workflow.domain.WorkflowRunStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkflowRunResponse(
        UUID id,
        UUID workflowDefinitionId,
        UUID conversationId,
        WorkflowRunStatus status,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage,
        String waitingAtNodeId) {}
