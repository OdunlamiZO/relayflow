package com.relayflow.api.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** The {@code button_reply} object inside an inbound interactive message. */
public record WhatsAppButtonReply(
        @JsonProperty("id") String id, @JsonProperty("title") String title) {}
