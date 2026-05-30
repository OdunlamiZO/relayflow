package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.WorkspacePermission;
import com.relayflow.api.messaging.domain.WorkspaceRole;
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
