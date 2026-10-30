package io.odunlamizo.relayflow.contact.dto;

// Docs: docs/codebase-components.md#contact-dto-records

import java.util.LinkedHashMap;
import java.util.Map;

public record UpdateContactCustomFieldsRequest(Map<String, String> customFields) {

    public UpdateContactCustomFieldsRequest {
        if (customFields == null) customFields = new LinkedHashMap<>();
    }
}
