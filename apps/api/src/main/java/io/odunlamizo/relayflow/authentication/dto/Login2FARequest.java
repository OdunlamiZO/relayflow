package io.odunlamizo.relayflow.authentication.dto;

// Docs: docs/codebase-components.md#auth-dto-records

import jakarta.validation.constraints.NotBlank;

public record Login2FARequest(@NotBlank String challengeToken, @NotBlank String otp) {}
