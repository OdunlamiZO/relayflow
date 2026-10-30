package io.odunlamizo.relayflow.profile.dto;

// Docs: docs/codebase-components.md#profile-dto-records

import jakarta.validation.constraints.NotBlank;

public record OtpRequest(@NotBlank String otp) {}
