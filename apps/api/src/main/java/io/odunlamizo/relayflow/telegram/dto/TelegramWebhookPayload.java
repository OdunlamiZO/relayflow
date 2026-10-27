package io.odunlamizo.relayflow.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramWebhookPayload(
        @JsonProperty("update_id") Long updateId,
        @JsonProperty("message") TelegramMessage message) {}
