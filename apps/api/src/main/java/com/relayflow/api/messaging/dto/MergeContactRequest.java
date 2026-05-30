package com.relayflow.api.messaging.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MergeContactRequest(@NotNull UUID sourceContactId) {}
