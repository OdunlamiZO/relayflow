package com.relayflow.api.hook.dto;

import com.relayflow.api.hook.HookOutcomeStatus;
import java.util.List;
import java.util.Map;

public record TestHookResponse(
        HookOutcomeStatus status,
        Object value,
        String errorMessage,
        Map<String, Object> variables,
        List<String> warnings) {}
