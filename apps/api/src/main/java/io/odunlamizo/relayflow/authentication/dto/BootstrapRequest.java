package io.odunlamizo.relayflow.authentication.dto;

// Docs: docs/codebase-components.md#auth-dto-records

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BootstrapRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
                String password,
        @NotBlank String workspaceName) {}
