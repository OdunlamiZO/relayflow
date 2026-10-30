package io.odunlamizo.relayflow.workspace.dto;

// Docs: docs/codebase-components.md#workspace-dto-records

import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record InviteMemberRequest(
        @NotBlank @Email String email, @NotNull Set<WorkspacePermission> permissions) {}
