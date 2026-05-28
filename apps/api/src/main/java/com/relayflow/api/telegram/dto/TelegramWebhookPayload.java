package com.relayflow.api.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramWebhookPayload(
        @JsonProperty("update_id") Long updateId,
        @JsonProperty("message") TelegramMessage message) {}
