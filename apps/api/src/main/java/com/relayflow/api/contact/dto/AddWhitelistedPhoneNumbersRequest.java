package com.relayflow.api.contact.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AddWhitelistedPhoneNumbersRequest(@NotEmpty List<String> phoneNumbers) {}
