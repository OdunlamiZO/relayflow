package io.odunlamizo.relayflow.contact.dto;

// Docs: docs/codebase-components.md#contact-dto-records

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AddWhitelistedPhoneNumbersRequest(@NotEmpty List<String> phoneNumbers) {}
