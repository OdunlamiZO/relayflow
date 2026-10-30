package io.odunlamizo.relayflow.authentication.dto;

// Docs: docs/codebase-components.md#auth-dto-records

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
