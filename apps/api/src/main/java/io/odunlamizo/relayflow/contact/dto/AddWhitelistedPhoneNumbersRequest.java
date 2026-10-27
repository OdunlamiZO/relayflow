package io.odunlamizo.relayflow.contact.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AddWhitelistedPhoneNumbersRequest(@NotEmpty List<String> phoneNumbers) {}
