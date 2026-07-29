package com.relayflow.api.sse;

import java.util.Map;
import java.util.UUID;

/**
 * Application event that triggers an SSE broadcast to all subscribers of a workspace.
 *
 * <p>Publish this via {@link org.springframework.context.ApplicationEventPublisher} from within a
 * transaction — the {@link WorkspaceSseService} listener fires it with
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} so the push only reaches clients after
 * the DB write is durable and visible to subsequent reads.
 */
public record SseBroadcastEvent(
        UUID workspaceId, SseEventType eventType, Map<String, Object> payload) {}
