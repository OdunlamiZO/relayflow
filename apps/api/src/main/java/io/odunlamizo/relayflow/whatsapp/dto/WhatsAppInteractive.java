package io.odunlamizo.relayflow.whatsapp.dto;

// Docs: docs/codebase-components.md#whatsapp-dto-records

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppInteractive(
        @JsonProperty("type") String type,
        @JsonProperty("button_reply") WhatsAppButtonReply buttonReply) {}
