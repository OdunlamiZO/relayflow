package io.odunlamizo.relayflow.contact.dto;

// Docs: docs/codebase-components.md#contact-dto-records

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateContactRequest(@NotNull UUID workspaceId, String displayName) {}
