package com.relayflow.api.messaging.domain;

public record ContactFieldDefinition(String key, String label, String description) {

    public ContactFieldDefinition {
        if (description == null) description = "";
    }
}
