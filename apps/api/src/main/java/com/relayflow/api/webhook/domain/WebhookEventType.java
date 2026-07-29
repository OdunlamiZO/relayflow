package com.relayflow.api.webhook.domain;

import lombok.Getter;

/** Events that can be delivered to a workspace webhook endpoint. */
@Getter
public enum WebhookEventType {
    CONTACT_CREATED("contact.created"),
    CONTACT_UPDATED("contact.updated");

    private final String eventName;

    WebhookEventType(String eventName) {
        this.eventName = eventName;
    }
}
