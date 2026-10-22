package io.odunlamizo.relayflow.webhook.dto;

import io.odunlamizo.relayflow.webhook.domain.WebhookEventType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** generatedSecret is set only on the call that creates the webhook. */
public record WebhookConfigResponse(
        UUID id,
        UUID workspaceId,
        String url,
        boolean enabled,
        Set<WebhookEventType> events,
        Instant createdAt,
        Instant updatedAt,
        String generatedSecret) {}
