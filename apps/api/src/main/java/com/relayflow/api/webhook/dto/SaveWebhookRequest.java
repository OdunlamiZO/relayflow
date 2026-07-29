package com.relayflow.api.webhook.dto;

import com.relayflow.api.webhook.domain.WebhookEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.hibernate.validator.constraints.URL;

public record SaveWebhookRequest(
        @NotBlank @URL @Size(max = 2048) String url,

        /**
         * Plaintext secret used to sign webhook payloads (HMAC-SHA256). Will be stored encrypted;
         * never returned in API responses. Omit (null) on update calls when the secret should
         * remain unchanged.
         */
        @Size(min = 16, max = 256) String secret,
        boolean enabled,
        @NotNull Set<WebhookEventType> events) {}
