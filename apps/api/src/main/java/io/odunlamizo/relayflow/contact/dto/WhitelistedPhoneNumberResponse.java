package io.odunlamizo.relayflow.contact.dto;

// Docs: docs/codebase-components.md#contact-dto-records

import java.time.Instant;
import java.util.UUID;

public record WhitelistedPhoneNumberResponse(UUID id, String phoneNumber, Instant createdAt) {}
