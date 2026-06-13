package com.relayflow.api.workflow.dto;

import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.domain.WorkflowRunStepStatus;
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
