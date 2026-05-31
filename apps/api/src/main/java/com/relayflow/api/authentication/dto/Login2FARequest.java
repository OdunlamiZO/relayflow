package com.relayflow.api.authentication.dto;

import jakarta.validation.constraints.NotBlank;

public record Login2FARequest(@NotBlank String challengeToken, @NotBlank String otp) {}
