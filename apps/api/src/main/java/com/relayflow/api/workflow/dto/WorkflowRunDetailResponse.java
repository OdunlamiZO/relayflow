package com.relayflow.api.workflow.dto;

import com.relayflow.api.workflow.domain.WorkflowRunStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowRunDetailResponse(
        UUID id,
        UUID workflowDefinitionId,
        UUID conversationId,
        WorkflowRunStatus status,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage,
        String waitingAtNodeId,
        List<WorkflowRunStepResponse> steps) {}
