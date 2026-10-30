package io.odunlamizo.relayflow.telegram.dto;

// Docs: docs/codebase-components.md#telegram-dto-records

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramMessage(
        @JsonProperty("message_id") Long messageId,
        @JsonProperty("from") TelegramUser from,
        @JsonProperty("chat") TelegramChat chat,
        @JsonProperty("text") String text,
        @JsonProperty("contact") TelegramContact contact) {}
