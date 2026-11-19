package io.odunlamizo.relayflow.whatsapp.dto;

// Docs: docs/codebase-components.md#whatsapp-dto-records

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppAudio(
        @JsonProperty("id") String id,
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("voice") Boolean voice) {}
