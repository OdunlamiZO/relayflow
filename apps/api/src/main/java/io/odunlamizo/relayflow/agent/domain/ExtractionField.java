package io.odunlamizo.relayflow.agent.domain;

// Docs: docs/codebase-components.md#ai-agent-domain-records

public record ExtractionField(String key, String description, String validationHook) {

    public ExtractionField(String key, String description) {
        this(key, description, null);
    }
}
