package io.odunlamizo.relayflow.workspace.dto;

// Docs: docs/codebase-components.md#workspace-dto-records

import io.odunlamizo.relayflow.workspace.domain.ContactTagDefinition;
import java.util.ArrayList;
import java.util.List;

public record UpdateContactTagDefinitionsRequest(List<ContactTagDefinition> contactTagDefinitions) {

    public UpdateContactTagDefinitionsRequest {
        if (contactTagDefinitions == null) contactTagDefinitions = new ArrayList<>();
    }
}
