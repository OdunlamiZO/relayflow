package com.relayflow.api.contact.dto;

import java.time.Instant;
import java.util.UUID;

public record WhitelistedPhoneNumberResponse(UUID id, String phoneNumber, Instant createdAt) {}
