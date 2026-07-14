package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.ContactFieldDefinition;
import java.util.ArrayList;
import java.util.List;

public record UpdateContactFieldDefinitionsRequest(
        List<ContactFieldDefinition> contactFieldDefinitions) {

    public UpdateContactFieldDefinitionsRequest {
        if (contactFieldDefinitions == null) contactFieldDefinitions = new ArrayList<>();
    }
}
