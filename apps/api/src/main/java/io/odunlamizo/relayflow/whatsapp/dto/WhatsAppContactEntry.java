package io.odunlamizo.relayflow.whatsapp.dto;

// Docs: docs/codebase-components.md#whatsapp-dto-records

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppContactEntry(
        @JsonProperty("profile") WhatsAppContactProfile profile,
        @JsonProperty("wa_id") String whatsAppId) {}
