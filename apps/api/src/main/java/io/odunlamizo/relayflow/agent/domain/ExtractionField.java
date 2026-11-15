package io.odunlamizo.relayflow.agent.domain;

// Docs: docs/codebase-components.md#ai-agent-domain-records

public record ExtractionField(
        String key, String description, String validationHook, Boolean askIfMissing) {

    public ExtractionField {
        if (askIfMissing == null) {
            askIfMissing = true;
        }
    }

    public ExtractionField(String key, String description, String validationHook) {
        this(key, description, validationHook, true);
    }

    public ExtractionField(String key, String description) {
        this(key, description, null, true);
    }
}
