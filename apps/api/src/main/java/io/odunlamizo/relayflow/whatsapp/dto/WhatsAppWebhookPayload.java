package io.odunlamizo.relayflow.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WhatsAppWebhookPayload(
        @JsonProperty("object") String object, @JsonProperty("entry") List<WhatsAppEntry> entry) {}
