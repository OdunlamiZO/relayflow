package io.odunlamizo.relayflow.workspace.dto;

import io.odunlamizo.relayflow.workspace.domain.WorkspaceInvite.InviteStatus;
import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record InvitePreviewResponse(
        UUID inviteId,
        UUID workspaceId,
        String workspaceName,
        String inviterName,
        String email,
        Set<WorkspacePermission> permissions,
        InviteStatus status,
        Instant expiresAt,
        boolean accountExists) {}
