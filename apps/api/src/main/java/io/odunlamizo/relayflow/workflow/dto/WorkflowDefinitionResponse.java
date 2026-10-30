package io.odunlamizo.relayflow.workflow.dto;

// Docs: docs/codebase-components.md#workflow-dto-records

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WorkflowDefinitionResponse(
        UUID id,
        UUID workspaceId,
        String name,
        boolean enabled,
        Map<String, Object> draftGraph,
        Instant createdAt,
        Instant updatedAt) {}
