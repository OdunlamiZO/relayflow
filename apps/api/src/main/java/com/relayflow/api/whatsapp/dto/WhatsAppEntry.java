package com.relayflow.api.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WhatsAppEntry(
        @JsonProperty("id") String id, @JsonProperty("changes") List<WhatsAppChange> changes) {}
