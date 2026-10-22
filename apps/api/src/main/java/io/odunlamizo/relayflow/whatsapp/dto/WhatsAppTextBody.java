package io.odunlamizo.relayflow.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppTextBody(@JsonProperty("body") String body) {}
