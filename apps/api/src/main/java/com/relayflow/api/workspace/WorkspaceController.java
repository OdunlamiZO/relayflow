package com.relayflow.api.workspace;

import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import com.relayflow.api.workspace.dto.CreateWorkspaceRequest;
import com.relayflow.api.workspace.dto.InviteMemberRequest;
import com.relayflow.api.workspace.dto.UpdateContactFieldDefinitionsRequest;
import com.relayflow.api.workspace.dto.UpdateMemberRequest;
import com.relayflow.api.workspace.dto.UpdateWorkspaceRequest;
import com.relayflow.api.workspace.dto.WorkspaceMemberResponse;
import com.relayflow.api.workspace.dto.WorkspaceResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final SecurityUtils securityUtils;
    private final WorkspaceAuthorizationService authorizationService;

    public WorkspaceController(
            WorkspaceService workspaceService,
            SecurityUtils securityUtils,
            WorkspaceAuthorizationService authorizationService) {
        this.workspaceService = workspaceService;
        this.securityUtils = securityUtils;
        this.authorizationService = authorizationService;
    }

    // --- Workspaces ---

    @GetMapping("/workspaces")
    List<WorkspaceResponse> listWorkspaces(Authentication authentication) {
        UUID userId = securityUtils.resolveUserId(authentication);

        return workspaceService.listWorkspaces(userId);
    }

    @PostMapping("/workspaces")
    @ResponseStatus(HttpStatus.CREATED)
    WorkspaceResponse createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request, Authentication authentication) {
        authorizationService.assertOwnerOfAnyWorkspace(authentication);

        UUID userId = securityUtils.resolveUserId(authentication);

        return workspaceService.createWorkspace(request, userId);
    }

    @PatchMapping("/workspaces/{workspaceId}")
    WorkspaceResponse updateWorkspace(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        return workspaceService.updateWorkspace(workspaceId, request.name());
    }

    @PutMapping("/workspaces/{workspaceId}/contact-field-definitions")
    WorkspaceResponse updateContactFieldDefinitions(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateContactFieldDefinitionsRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.CONTACT_FIELDS_WRITE);

        return workspaceService.updateContactFieldDefinitions(
                workspaceId, request.contactFieldDefinitions());
    }

    // --- Workspace Members ---

    @GetMapping("/workspaces/{workspaceId}/members")
    List<WorkspaceMemberResponse> listMembers(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertMember(workspaceId, authentication);

        return workspaceService.listWorkspaceMembers(workspaceId);
    }

    @PostMapping("/workspaces/{workspaceId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    WorkspaceMemberResponse inviteMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody InviteMemberRequest request,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        return workspaceService.inviteWorkspaceMember(
                workspaceId, request.email(), request.permissions());
    }

    @PatchMapping("/workspaces/{workspaceId}/members/{memberId}")
    WorkspaceMemberResponse updateMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRequest request,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        return workspaceService.updateWorkspaceMember(
                workspaceId, memberId, request.role(), request.permissions());
    }

    @DeleteMapping("/workspaces/{workspaceId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        workspaceService.removeWorkspaceMember(workspaceId, memberId);
    }

    @PostMapping("/workspaces/{workspaceId}/members/{memberId}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void generatePasswordResetForMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        workspaceService.generatePasswordResetForMember(workspaceId, memberId);
    }

    @PutMapping("/workspaces/{workspaceId}/owner")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void transferOwnership(
            @PathVariable UUID workspaceId,
            @RequestParam UUID memberId,
            Authentication authentication) {
        UUID callerId = authorizationService.getUser(authentication);

        workspaceService.transferOwnership(workspaceId, memberId, callerId);
    }
}
