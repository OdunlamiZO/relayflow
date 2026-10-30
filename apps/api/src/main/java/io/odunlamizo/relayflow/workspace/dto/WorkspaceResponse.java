package io.odunlamizo.relayflow.workspace.dto;

// Docs: docs/codebase-components.md#workspace-dto-records

import io.odunlamizo.relayflow.workspace.domain.ContactAccess;
import io.odunlamizo.relayflow.workspace.domain.ContactFieldDefinition;
import io.odunlamizo.relayflow.workspace.domain.ContactTagDefinition;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        List<ContactFieldDefinition> contactFieldDefinitions,
        List<ContactTagDefinition> contactTagDefinitions,
        ContactAccess contactAccess,
        String phoneRegion,
        Instant createdAt) {}
