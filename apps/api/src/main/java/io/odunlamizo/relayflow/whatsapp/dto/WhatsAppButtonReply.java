package io.odunlamizo.relayflow.whatsapp.dto;

// Docs: docs/codebase-components.md#whatsapp-dto-records

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppButtonReply(
        @JsonProperty("id") String id, @JsonProperty("title") String title) {}
