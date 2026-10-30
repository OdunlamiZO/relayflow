package io.odunlamizo.relayflow.hook.dto;

// Docs: docs/codebase-components.md#hook

public record BuiltInHookResponse(
        String key, String name, String description, String expression, String errorMessage) {}
