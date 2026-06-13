package com.relayflow.api.messaging;

import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.messaging.dto.CreateInviteRequest;
import com.relayflow.api.messaging.dto.InvitePreviewResponse;
import com.relayflow.api.messaging.dto.WorkspaceInviteResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class WorkspaceInviteController {

    private final WorkspaceInviteService inviteService;

    private final WorkspaceAuthorizationService authorizationService;

    private final SecurityUtils securityUtils;

    public WorkspaceInviteController(
            WorkspaceInviteService inviteService,
            WorkspaceAuthorizationService authorizationService,
            SecurityUtils securityUtils) {
        this.inviteService = inviteService;
        this.authorizationService = authorizationService;
        this.securityUtils = securityUtils;
    }

    /** List pending invites — owner only. */
    @GetMapping("/workspaces/{workspaceId}/invites")
    List<WorkspaceInviteResponse> listInvites(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        return inviteService.listPendingInvites(workspaceId);
    }

    /** Create (or refresh) an invite — owner only. */
    @PostMapping("/workspaces/{workspaceId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    WorkspaceInviteResponse createInvite(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateInviteRequest request,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        UUID inviterId = securityUtils.resolveUserId(authentication);

        return inviteService.createInvite(workspaceId, inviterId, request);
    }

    /** Revoke a pending invite — owner only. */
    @DeleteMapping("/workspaces/{workspaceId}/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revokeInvite(
            @PathVariable UUID workspaceId,
            @PathVariable UUID inviteId,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        inviteService.revokeInvite(workspaceId, inviteId);
    }

    /** Public preview — no authentication required. */
    @GetMapping("/invites/{token}")
    InvitePreviewResponse previewInvite(@PathVariable UUID token) {
        return inviteService.getPreview(token);
    }

    /** Accept an invite — requires authentication; email must match. */
    @PostMapping("/invites/{token}/accept")
    @ResponseStatus(HttpStatus.CREATED)
    InviteAcceptedResponse acceptInvite(@PathVariable UUID token, Authentication authentication) {
        UUID userId = securityUtils.resolveUserId(authentication);
        UUID workspaceId = inviteService.acceptInvite(token, userId);

        return new InviteAcceptedResponse(workspaceId);
    }

    record InviteAcceptedResponse(UUID workspaceId) {}
}
