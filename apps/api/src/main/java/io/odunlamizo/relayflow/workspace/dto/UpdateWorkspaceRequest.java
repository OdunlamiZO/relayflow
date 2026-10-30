package io.odunlamizo.relayflow.workspace.dto;

// Docs: docs/codebase-components.md#workspace-dto-records

import jakarta.validation.constraints.NotBlank;

public record UpdateWorkspaceRequest(@NotBlank String name) {}
