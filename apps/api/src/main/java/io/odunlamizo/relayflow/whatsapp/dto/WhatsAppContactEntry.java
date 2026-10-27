package io.odunlamizo.relayflow.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppContactEntry(
        @JsonProperty("profile") WhatsAppContactProfile profile,
        @JsonProperty("wa_id") String whatsAppId) {}
