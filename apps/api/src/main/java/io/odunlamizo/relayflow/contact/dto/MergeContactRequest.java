package io.odunlamizo.relayflow.contact.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MergeContactRequest(@NotNull UUID sourceContactId) {}
