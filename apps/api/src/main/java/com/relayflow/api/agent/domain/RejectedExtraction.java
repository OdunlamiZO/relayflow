package com.relayflow.api.agent.domain;

import java.util.Map;

/** An extracted value its field's hook turned down; {@code reason} is written for the contact. */
public record RejectedExtraction(String key, String value, String reason) {

    public String conversionRequest() {
        return "Field: " + key + "\nCustomer's answer: \"" + value + "\"\nRejected with: " + reason;
    }

    public Map<String, String> toSnapshot() {
        return Map.of("key", key, "value", value, "reason", reason);
    }
}
