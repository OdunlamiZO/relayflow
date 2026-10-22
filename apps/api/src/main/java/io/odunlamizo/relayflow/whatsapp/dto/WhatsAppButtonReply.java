package io.odunlamizo.relayflow.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppButtonReply(
        @JsonProperty("id") String id, @JsonProperty("title") String title) {}
