package com.relayflow.api.workspace.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateApiKeyRequest(
        @NotBlank @Size(max = 100) String name, @Future Instant expiresAt) {}
