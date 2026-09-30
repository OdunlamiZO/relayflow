package com.relayflow.api.hook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record TestHookRequest(
        @NotBlank @Size(max = 4000) String expression,
        @NotNull @Size(max = 4000) String value,
        Map<String, Object> variables) {}
