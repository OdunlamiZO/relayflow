package io.odunlamizo.relayflow.webhook.dto;

// Docs: docs/codebase-components.md#contactmergedpayload

import java.util.Map;

public record ContactMergedPayload(
        Map<String, Object> contact, Map<String, Object> mergedContact) {}
