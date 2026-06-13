package com.relayflow.api.messaging.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateWorkspaceRequest(@NotBlank String name) {}
