package io.odunlamizo.relayflow.whatsapp.dto;

// Docs: docs/codebase-components.md#whatsapp-dto-records

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppChange(
        @JsonProperty("value") WhatsAppValue value, @JsonProperty("field") String field) {}
