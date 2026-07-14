package com.relayflow.api.messaging;

import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.messaging.domain.WorkspaceMember;
import com.relayflow.api.messaging.domain.WorkspacePermission;
import com.relayflow.api.messaging.domain.WorkspaceRole;
import com.relayflow.api.messaging.repository.WorkspaceMemberRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Checks that the authenticated user has the required role or permission for a workspace.
 *
 * <p>Owners bypass all permission checks — they implicitly hold every permission. Non-members
 * receive 403 Forbidden regardless of what permission is requested.
 */
@Service
public class WorkspaceAuthorizationService {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    private final SecurityUtils securityUtils;

    public WorkspaceAuthorizationService(
            WorkspaceMemberRepository workspaceMemberRepository, SecurityUtils securityUtils) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Asserts that the authenticated user holds {@code permission} in {@code workspaceId}.
     *
     * @throws ResponseStatusException 403 if not a member or permission not granted
     */
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

    /**
     * Asserts that the authenticated user is a member of {@code workspaceId} (any role).
     *
     * @throws ResponseStatusException 403 if not a member
     */
    public void assertMember(UUID workspaceId, Authentication authentication) {
        resolveMember(workspaceId, authentication);
    }

    /**
     * Asserts that the authenticated user is an OWNER of {@code workspaceId}.
     *
     * @throws ResponseStatusException 403 if not an owner
     */
    public void assertOwner(UUID workspaceId, Authentication authentication) {
        WorkspaceMember member = resolveMember(workspaceId, authentication);

        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace owner required");
        }
    }

    /**
     * Asserts that the authenticated user is an OWNER of at least one workspace — gates creating
     * additional workspaces to existing owners.
     *
     * @throws ResponseStatusException 403 if the user owns no workspace
     */
    public void assertOwnerOfAnyWorkspace(Authentication authentication) {
        UUID userId = securityUtils.resolveUserId(authentication);

        if (!workspaceMemberRepository.existsByUserAndRole(userId, WorkspaceRole.OWNER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace owner required");
        }
    }

    /** Returns the user ID of the authenticated principal. */
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
