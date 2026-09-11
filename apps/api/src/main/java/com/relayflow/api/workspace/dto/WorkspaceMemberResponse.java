package com.relayflow.api.workspace.dto;

import com.relayflow.api.workspace.domain.WorkspacePermission;
import com.relayflow.api.workspace.domain.WorkspaceRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID id,
        UUID userId,
        String email,
        String displayName,
        String avatarUrl,
        WorkspaceRole role,
        Set<WorkspacePermission> permissions,
        Instant joinedAt) {}
