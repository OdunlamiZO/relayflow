package com.relayflow.api.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateWorkflowDefinitionRequest(@NotNull UUID workspaceId, @NotBlank String name) {}
