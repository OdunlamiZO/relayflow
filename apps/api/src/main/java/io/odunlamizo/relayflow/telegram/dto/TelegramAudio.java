package io.odunlamizo.relayflow.telegram.dto;

// Docs: docs/codebase-components.md#telegram-dto-records

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramAudio(
        @JsonProperty("file_id") String fileId,
        @JsonProperty("duration") Integer duration,
        @JsonProperty("mime_type") String mimeType) {}
