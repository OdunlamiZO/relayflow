package com.relayflow.api.workspace.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateWorkspaceRequest(@NotBlank String name) {}
