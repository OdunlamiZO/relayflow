package io.odunlamizo.relayflow.workspace.dto;

import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceRole;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UpdateMemberRequest(
        WorkspaceRole role, @NotNull Set<WorkspacePermission> permissions) {}
