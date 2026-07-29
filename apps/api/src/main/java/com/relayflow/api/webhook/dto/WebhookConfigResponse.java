package com.relayflow.api.webhook.dto;

import com.relayflow.api.webhook.domain.WebhookEventType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Public representation of a workspace webhook configuration.
 *
 * <p>The secret is never included in this response; clients that need it must use the rotate-secret
 * endpoint which returns the new plaintext value once.
 */
public record WebhookConfigResponse(
        UUID id,
        UUID workspaceId,
        String url,
        boolean enabled,
        Set<WebhookEventType> events,
        Instant createdAt,
        Instant updatedAt) {}
