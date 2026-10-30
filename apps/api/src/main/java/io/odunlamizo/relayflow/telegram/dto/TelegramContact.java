package io.odunlamizo.relayflow.telegram.dto;

// Docs: docs/codebase-components.md#telegram-dto-records

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramContact(
        @JsonProperty("phone_number") String phoneNumber, @JsonProperty("user_id") Long userId) {}
