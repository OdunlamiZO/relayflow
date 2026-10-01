package com.relayflow.api.workspace.dto;

import com.relayflow.api.workspace.domain.ContactTagDefinition;
import java.util.ArrayList;
import java.util.List;

public record UpdateContactTagDefinitionsRequest(List<ContactTagDefinition> contactTagDefinitions) {

    public UpdateContactTagDefinitionsRequest {
        if (contactTagDefinitions == null) contactTagDefinitions = new ArrayList<>();
    }
}
