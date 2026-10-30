package io.odunlamizo.relayflow.messaging.dto;

// Docs: docs/codebase-components.md#messaging-dto-records

import java.util.UUID;

/** A null assigneeId unassigns the conversation. */
public record UpdateAssigneeRequest(UUID assigneeId) {}
