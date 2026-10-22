package io.odunlamizo.relayflow.workspace.dto;

import io.odunlamizo.relayflow.workspace.domain.WorkspaceInvite.InviteStatus;
import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
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
