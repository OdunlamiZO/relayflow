package io.odunlamizo.relayflow.workspace.dto;

// Docs: docs/codebase-components.md#workspace-dto-records

import io.odunlamizo.relayflow.workspace.domain.ContactFieldDefinition;
import java.util.ArrayList;
import java.util.List;

public record UpdateContactFieldDefinitionsRequest(
        List<ContactFieldDefinition> contactFieldDefinitions) {

    public UpdateContactFieldDefinitionsRequest {
        if (contactFieldDefinitions == null) contactFieldDefinitions = new ArrayList<>();
    }
}
