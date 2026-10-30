package io.odunlamizo.relayflow.whatsapp.dto;

// Docs: docs/codebase-components.md#whatsapp-dto-records

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WhatsAppWebhookPayload(
        @JsonProperty("object") String object, @JsonProperty("entry") List<WhatsAppEntry> entry) {}
