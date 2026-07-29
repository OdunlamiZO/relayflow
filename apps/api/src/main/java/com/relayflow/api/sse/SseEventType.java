package com.relayflow.api.sse;

/** Events broadcast to workspace SSE subscribers. */
public enum SseEventType {
    MESSAGE_CREATED("message.created"),
    CONVERSATION_UPDATED("conversation.updated"),
    AI_DRAFT_CREATED("ai.draft.created"),
    AI_ESCALATED("ai.escalated");

    private final String eventName;

    SseEventType(String eventName) {
        this.eventName = eventName;
    }

    /** The dot-notation name sent as the SSE event's {@code event:} field. */
    public String getEventName() {
        return eventName;
    }
}
