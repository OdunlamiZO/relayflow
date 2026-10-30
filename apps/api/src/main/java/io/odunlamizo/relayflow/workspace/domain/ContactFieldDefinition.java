package io.odunlamizo.relayflow.workspace.domain;

// Docs: docs/codebase-components.md#contactfielddefinition

public record ContactFieldDefinition(String key, String label, String description) {

    public ContactFieldDefinition {
        if (description == null) description = "";
    }
}
