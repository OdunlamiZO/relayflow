package com.relayflow.api.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WhatsAppValue(
        @JsonProperty("messaging_product") String messagingProduct,
        @JsonProperty("contacts") List<WhatsAppContactEntry> contacts,
        @JsonProperty("messages") List<WhatsAppMessage> messages) {}
