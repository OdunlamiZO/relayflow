package com.relayflow.api.workflow.dto;

import com.relayflow.api.workflow.domain.WorkflowRunStatus;
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
