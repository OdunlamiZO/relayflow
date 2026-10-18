package com.relayflow.api.messaging.dto;

import java.util.UUID;

/** A null assigneeId unassigns the conversation. */
public record UpdateAssigneeRequest(UUID assigneeId) {}
