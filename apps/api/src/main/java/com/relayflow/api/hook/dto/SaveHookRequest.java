package com.relayflow.api.hook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveHookRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @NotBlank @Size(max = 4000) String expression,
        @NotBlank @Size(max = 500) String errorMessage) {}
