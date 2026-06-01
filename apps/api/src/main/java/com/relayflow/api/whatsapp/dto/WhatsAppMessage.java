package com.relayflow.api.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppMessage(
        @JsonProperty("id") String id,
        @JsonProperty("from") String from,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("type") String type,
        @JsonProperty("text") WhatsAppTextBody text) {}
