package com.relayflow.api.workspace.domain;

public record ContactFieldDefinition(String key, String label, String description) {

    public ContactFieldDefinition {
        if (description == null) description = "";
    }
}
