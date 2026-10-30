package io.odunlamizo.relayflow.profile.dto;

// Docs: docs/codebase-components.md#profile-dto-records

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@NotBlank @Size(max = 200) String displayName) {}
