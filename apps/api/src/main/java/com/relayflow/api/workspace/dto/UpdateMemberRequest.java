package com.relayflow.api.workspace.dto;

import com.relayflow.api.workspace.domain.WorkspacePermission;
import com.relayflow.api.workspace.domain.WorkspaceRole;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UpdateMemberRequest(
        WorkspaceRole role, @NotNull Set<WorkspacePermission> permissions) {}
