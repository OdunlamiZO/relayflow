package com.relayflow.api.profile.dto;

import jakarta.validation.constraints.NotBlank;

/** Used for both enable-2FA and disable-2FA — both need only the current OTP code. */
public record OtpRequest(@NotBlank String otp) {}
