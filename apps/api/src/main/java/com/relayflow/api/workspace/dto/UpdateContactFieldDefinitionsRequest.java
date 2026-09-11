package com.relayflow.api.workspace.dto;

import com.relayflow.api.workspace.domain.ContactFieldDefinition;
import java.util.ArrayList;
import java.util.List;

public record UpdateContactFieldDefinitionsRequest(
        List<ContactFieldDefinition> contactFieldDefinitions) {

    public UpdateContactFieldDefinitionsRequest {
        if (contactFieldDefinitions == null) contactFieldDefinitions = new ArrayList<>();
    }
}
