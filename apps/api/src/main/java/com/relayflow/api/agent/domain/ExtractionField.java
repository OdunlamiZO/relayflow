package com.relayflow.api.agent.domain;

public record ExtractionField(String key, String description, String validationHook) {

    public ExtractionField(String key, String description) {
        this(key, description, null);
    }
}
