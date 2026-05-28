package com.relayflow.api.messaging.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateContactRequest(@NotNull UUID workspaceId, String displayName) {}
