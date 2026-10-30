package io.odunlamizo.relayflow.sse;

// Docs: docs/codebase-components.md#sseeventtype

public enum SseEventType {
    MESSAGE_CREATED("message.created"),
    CONVERSATION_UPDATED("conversation.updated"),
    AI_DRAFT_CREATED("ai.draft.created"),
    AI_ESCALATED("ai.escalated");

    private final String eventName;

    SseEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
