package com.relayflow.api.messaging.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record UpdateContactCustomFieldsRequest(Map<String, String> customFields) {

    public UpdateContactCustomFieldsRequest {
        if (customFields == null) customFields = new LinkedHashMap<>();
    }
}
