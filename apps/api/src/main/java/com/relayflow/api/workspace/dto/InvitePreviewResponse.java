package com.relayflow.api.workspace.dto;

import com.relayflow.api.workspace.domain.WorkspaceInvite.InviteStatus;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Public response for the invite preview page — no authentication required. */
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
