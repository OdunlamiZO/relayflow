package io.odunlamizo.relayflow.hook.dto;

// Docs: docs/codebase-components.md#hook

import io.odunlamizo.relayflow.hook.domain.HookOutcomeStatus;
import java.util.List;
import java.util.Map;

public record TestHookResponse(
        HookOutcomeStatus status,
        Object value,
        String errorMessage,
        Map<String, Object> variables,
        List<String> warnings) {}
