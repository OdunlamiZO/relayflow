package com.relayflow.api.workspace;

import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.workspace.domain.WorkspaceMember;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import com.relayflow.api.workspace.domain.WorkspaceRole;
import com.relayflow.api.workspace.repository.WorkspaceMemberRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkspaceAuthorizationService {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    private final SecurityUtils securityUtils;

    public WorkspaceAuthorizationService(
            WorkspaceMemberRepository workspaceMemberRepository, SecurityUtils securityUtils) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.securityUtils = securityUtils;
    }

    public void assertPermission(
            UUID workspaceId, Authentication authentication, WorkspacePermission permission) {
        WorkspaceMember member = resolveMember(workspaceId, authentication);

        if (member.getRole() == WorkspaceRole.OWNER) {
            return;
        }

        if (!member.getPermissions().contains(permission)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Missing permission: " + permission);
        }
    }

    public void assertMember(UUID workspaceId, Authentication authentication) {
        resolveMember(workspaceId, authentication);
    }

    public void assertOwner(UUID workspaceId, Authentication authentication) {
        WorkspaceMember member = resolveMember(workspaceId, authentication);

        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace owner required");
        }
    }

    public void assertOwnerOfAnyWorkspace(Authentication authentication) {
        UUID userId = securityUtils.resolveUserId(authentication);

        if (!workspaceMemberRepository.existsByUserAndRole(userId, WorkspaceRole.OWNER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace owner required");
        }
    }

    public UUID getUser(Authentication authentication) {
        return securityUtils.resolveUserId(authentication);
    }

    private WorkspaceMember resolveMember(UUID workspaceId, Authentication authentication) {
        UUID userId = securityUtils.resolveUserId(authentication);

        return workspaceMemberRepository
                .findByWorkspaceAndUser(workspaceId, userId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.FORBIDDEN, "Not a member of this workspace"));
    }
}
