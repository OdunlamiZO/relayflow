package com.relayflow.api.webhook.dto;

import com.relayflow.api.webhook.domain.WebhookEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.hibernate.validator.constraints.URL;

/** The signing secret is never set here — it's always server-generated (create) or rotated. */
public record SaveWebhookRequest(
        @NotBlank @URL @Size(max = 2048) String url,
        boolean enabled,
        @NotNull Set<WebhookEventType> events) {}
