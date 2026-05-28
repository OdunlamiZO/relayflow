package com.relayflow.api.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramChat(@JsonProperty("id") Long id, @JsonProperty("type") String type) {}
