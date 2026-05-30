package com.relayflow.api.messaging.dto;

import com.relayflow.api.messaging.domain.WorkspacePermission;
import com.relayflow.api.messaging.domain.WorkspaceRole;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UpdateMemberRequest(
        WorkspaceRole role, @NotNull Set<WorkspacePermission> permissions) {}
