package io.odunlamizo.relayflow.contact.dto;

// Docs: docs/codebase-components.md#contact-dto-records

import java.util.LinkedHashMap;
import java.util.Map;

public record UpdateContactTagsRequest(Map<String, String> tags) {

    public UpdateContactTagsRequest {
        if (tags == null) tags = new LinkedHashMap<>();
    }
}
