package com.relayflow.api.contact.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateContactRequest(@NotNull UUID workspaceId, String displayName) {}
