package com.relayflow.api.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppContactProfile(@JsonProperty("name") String name) {}
