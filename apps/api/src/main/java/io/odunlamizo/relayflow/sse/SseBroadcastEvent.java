package io.odunlamizo.relayflow.sse;

import java.util.Map;
import java.util.UUID;

public record SseBroadcastEvent(
        UUID workspaceId, SseEventType eventType, Map<String, Object> payload) {}
