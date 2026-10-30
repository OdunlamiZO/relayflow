package io.odunlamizo.relayflow.workflow.dto;

// Docs: docs/codebase-components.md#workflow-dto-records

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateWorkflowDefinitionRequest(@NotNull UUID workspaceId, @NotBlank String name) {}
