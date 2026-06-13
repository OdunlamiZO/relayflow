package com.relayflow.api.messaging.dto;

import java.util.UUID;

/** {@code assigneeId} is nullable by design — {@code null} unassigns the conversation. */
public record UpdateAssigneeRequest(UUID assigneeId) {}
