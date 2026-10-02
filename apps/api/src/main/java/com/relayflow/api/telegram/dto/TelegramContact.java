package com.relayflow.api.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramContact(
        @JsonProperty("phone_number") String phoneNumber, @JsonProperty("user_id") Long userId) {}
