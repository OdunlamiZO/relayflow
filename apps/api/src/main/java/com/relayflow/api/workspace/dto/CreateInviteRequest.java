package com.relayflow.api.workspace.dto;

import com.relayflow.api.workspace.domain.WorkspacePermission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record CreateInviteRequest(
        @NotBlank @Email String email, @NotNull Set<WorkspacePermission> permissions) {}
