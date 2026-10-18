package com.relayflow.api.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveSecretRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$") String name,
        @NotBlank String value) {}
