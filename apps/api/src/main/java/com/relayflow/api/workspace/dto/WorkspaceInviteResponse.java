package com.relayflow.api.workspace.dto;

import com.relayflow.api.workspace.domain.WorkspaceInvite.InviteStatus;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record WorkspaceInviteResponse(
        UUID id,
        String email,
        String inviterName,
        Set<WorkspacePermission> permissions,
        InviteStatus status,
        Instant createdAt,
        Instant expiresAt) {}
