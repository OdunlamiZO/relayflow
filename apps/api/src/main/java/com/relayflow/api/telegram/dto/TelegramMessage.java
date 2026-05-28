package com.relayflow.api.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramMessage(
        @JsonProperty("message_id") Long messageId,
        @JsonProperty("from") TelegramUser from,
        @JsonProperty("chat") TelegramChat chat,
        @JsonProperty("text") String text) {}
