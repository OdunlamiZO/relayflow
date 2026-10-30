package io.odunlamizo.relayflow.workspace.dto;

// Docs: docs/codebase-components.md#workspace-dto-records

import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceRole;
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
