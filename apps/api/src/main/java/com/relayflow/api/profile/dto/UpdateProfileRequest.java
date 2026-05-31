package com.relayflow.api.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 200) String displayName, boolean receiveEmailUpdates) {}
