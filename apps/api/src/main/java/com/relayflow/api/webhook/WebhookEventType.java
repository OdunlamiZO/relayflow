package com.relayflow.api.webhook;

/** Events that can be delivered to a workspace webhook endpoint. */
public enum WebhookEventType {
    CONTACT_CREATED("contact.created");

    private final String eventName;

    WebhookEventType(String eventName) {
        this.eventName = eventName;
    }

    /** The dot-notation name used in the JSON payload (e.g. {@code "contact.created"}). */
    public String getEventName() {
        return eventName;
    }
}
