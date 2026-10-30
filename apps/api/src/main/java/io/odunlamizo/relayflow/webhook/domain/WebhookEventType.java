package io.odunlamizo.relayflow.webhook.domain;

// Docs: docs/codebase-components.md#webhookeventtype

import lombok.Getter;

@Getter
public enum WebhookEventType {
    CONTACT_CREATED("contact.created"),
    CONTACT_UPDATED("contact.updated"),
    CONTACT_DELETED("contact.deleted"),
    CONTACT_MERGED("contact.merged");

    private final String eventName;

    WebhookEventType(String eventName) {
        this.eventName = eventName;
    }
}
