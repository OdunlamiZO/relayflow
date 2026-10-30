package io.odunlamizo.relayflow.workflow.dto;

// Docs: docs/codebase-components.md#workflow-dto-records

import io.odunlamizo.relayflow.workflow.NodeType;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRunStepStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WorkflowRunStepResponse(
        UUID id,
        String nodeId,
        NodeType nodeType,
        WorkflowRunStepStatus status,
        Map<String, Object> inputSnapshot,
        Map<String, Object> outputSnapshot,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs) {}
