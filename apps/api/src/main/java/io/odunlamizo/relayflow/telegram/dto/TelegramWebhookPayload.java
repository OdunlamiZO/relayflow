package io.odunlamizo.relayflow.telegram.dto;

// Docs: docs/codebase-components.md#telegram-dto-records

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramWebhookPayload(
        @JsonProperty("update_id") Long updateId,
        @JsonProperty("message") TelegramMessage message) {}
