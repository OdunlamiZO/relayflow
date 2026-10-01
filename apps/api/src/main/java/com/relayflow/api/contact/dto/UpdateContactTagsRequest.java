package com.relayflow.api.contact.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record UpdateContactTagsRequest(Map<String, String> tags) {

    public UpdateContactTagsRequest {
        if (tags == null) tags = new LinkedHashMap<>();
    }
}
