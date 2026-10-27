package io.odunlamizo.relayflow.webhook.dto;

import java.util.Map;

public record ContactMergedPayload(
        Map<String, Object> contact, Map<String, Object> mergedContact) {}
