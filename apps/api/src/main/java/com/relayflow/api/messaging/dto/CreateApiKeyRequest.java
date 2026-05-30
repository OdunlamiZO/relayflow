package com.relayflow.api.messaging.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateApiKeyRequest(
        @NotBlank @Size(max = 100) String name,

        /**
         * Optional hard expiry timestamp. When null the key never expires. Must be a future instant
         * if provided.
         */
        @Future Instant expiresAt) {}
