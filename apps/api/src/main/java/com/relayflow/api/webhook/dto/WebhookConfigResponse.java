package com.relayflow.api.webhook.dto;

import com.relayflow.api.webhook.domain.WebhookEventType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Public representation of a workspace webhook configuration.
 *
 * <p>{@code generatedSecret} is only populated by the {@code PUT} call that creates the webhook;
 * it's {@code null} everywhere else (GET, and updates to an existing webhook).
 */
public record WebhookConfigResponse(
        UUID id,
        UUID workspaceId,
        String url,
        boolean enabled,
        Set<WebhookEventType> events,
        Instant createdAt,
        Instant updatedAt,
        String generatedSecret) {}
