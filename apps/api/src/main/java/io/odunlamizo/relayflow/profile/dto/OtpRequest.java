package io.odunlamizo.relayflow.profile.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpRequest(@NotBlank String otp) {}
