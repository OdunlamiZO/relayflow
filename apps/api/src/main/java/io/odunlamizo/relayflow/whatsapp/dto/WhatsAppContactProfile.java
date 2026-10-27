package io.odunlamizo.relayflow.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppContactProfile(@JsonProperty("name") String name) {}
