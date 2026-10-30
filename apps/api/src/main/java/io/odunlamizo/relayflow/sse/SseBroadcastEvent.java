package io.odunlamizo.relayflow.sse;

// Docs: docs/codebase-components.md#ssebroadcastevent

import java.util.Map;
import java.util.UUID;

public record SseBroadcastEvent(
        UUID workspaceId, SseEventType eventType, Map<String, Object> payload) {}
