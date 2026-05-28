package com.relayflow.api.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramUser(
        @JsonProperty("id") Long id,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("username") String username) {}
