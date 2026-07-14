package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.ContactFieldDefinition;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        List<ContactFieldDefinition> contactFieldDefinitions,
        Instant createdAt) {}
