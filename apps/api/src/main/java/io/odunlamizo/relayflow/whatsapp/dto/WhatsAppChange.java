package io.odunlamizo.relayflow.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppChange(
        @JsonProperty("value") WhatsAppValue value, @JsonProperty("field") String field) {}
