package com.relayflow.api.hook;

import java.util.List;
import java.util.Map;

public record HookOutcome(
        HookOutcomeStatus status,
        Object value,
        String errorMessage,
        Map<String, Object> variables,
        List<String> warnings) {

    public static HookOutcome accepted(
            Object value, Map<String, Object> variables, List<String> warnings) {
        return new HookOutcome(HookOutcomeStatus.ACCEPTED, value, null, variables, warnings);
    }

    public static HookOutcome rejected(String errorMessage, List<String> warnings) {
        return new HookOutcome(HookOutcomeStatus.REJECTED, null, errorMessage, Map.of(), warnings);
    }

    public static HookOutcome error(String errorMessage, List<String> warnings) {
        return new HookOutcome(HookOutcomeStatus.ERROR, null, errorMessage, Map.of(), warnings);
    }

    public boolean isAccepted() {
        return status == HookOutcomeStatus.ACCEPTED;
    }
}
