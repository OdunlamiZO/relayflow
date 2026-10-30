package io.odunlamizo.relayflow.workspace.dto;

// Docs: docs/codebase-components.md#workspace-dto-records

import io.odunlamizo.relayflow.workspace.domain.ContactAccess;
import jakarta.validation.constraints.NotNull;

public record UpdateContactAccessRequest(
        @NotNull ContactAccess contactAccess, String phoneRegion) {}
