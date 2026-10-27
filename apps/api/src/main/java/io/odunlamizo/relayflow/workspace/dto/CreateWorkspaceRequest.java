package io.odunlamizo.relayflow.workspace.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkspaceRequest(@NotBlank String name) {}
