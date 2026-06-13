package com.relayflow.api.authentication.dto;

import jakarta.validation.constraints.NotBlank;

public record GuestRecoveryRequest(@NotBlank String recoveryToken) {}
