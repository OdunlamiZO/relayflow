package com.relayflow.api.workspace.dto;

import com.relayflow.api.workspace.domain.ContactFieldDefinition;
import com.relayflow.api.workspace.domain.ContactTagDefinition;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        List<ContactFieldDefinition> contactFieldDefinitions,
        List<ContactTagDefinition> contactTagDefinitions,
        Instant createdAt) {}
