package com.relayflow.api.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The {@code interactive} object carried by inbound WhatsApp messages of {@code type=interactive}.
 * Currently only the {@code button_reply} sub-type is handled.
 */
public record WhatsAppInteractive(
        @JsonProperty("type") String type,
        @JsonProperty("button_reply") WhatsAppButtonReply buttonReply) {}
