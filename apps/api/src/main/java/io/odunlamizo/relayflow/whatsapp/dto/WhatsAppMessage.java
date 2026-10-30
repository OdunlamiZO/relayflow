package io.odunlamizo.relayflow.whatsapp.dto;

// Docs: docs/codebase-components.md#whatsapp-dto-records

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppMessage(
        @JsonProperty("id") String id,
        @JsonProperty("from") String from,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("type") String type,
        @JsonProperty("text") WhatsAppTextBody text,
        @JsonProperty("interactive") WhatsAppInteractive interactive) {}
