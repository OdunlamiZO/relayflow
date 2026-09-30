package com.relayflow.api.hook.dto;

public record BuiltInHookResponse(
        String key, String name, String description, String expression, String errorMessage) {}
