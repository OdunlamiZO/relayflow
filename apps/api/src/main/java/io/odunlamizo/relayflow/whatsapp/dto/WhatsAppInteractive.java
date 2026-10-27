package io.odunlamizo.relayflow.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppInteractive(
        @JsonProperty("type") String type,
        @JsonProperty("button_reply") WhatsAppButtonReply buttonReply) {}
