package io.odunlamizo.relayflow.agent.domain;

import java.util.Map;

public record RejectedExtraction(String key, String value, String reason) {

    public String conversionRequest() {
        return "Field: " + key + "\nCustomer's answer: \"" + value + "\"\nRejected with: " + reason;
    }

    public Map<String, String> toSnapshot() {
        return Map.of("key", key, "value", value, "reason", reason);
    }
}
