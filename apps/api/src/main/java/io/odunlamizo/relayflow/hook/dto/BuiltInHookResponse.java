package io.odunlamizo.relayflow.hook.dto;

public record BuiltInHookResponse(
        String key, String name, String description, String expression, String errorMessage) {}
