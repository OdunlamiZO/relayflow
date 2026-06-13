package com.relayflow.api.messaging;

import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.messaging.domain.WorkspacePermission;
import com.relayflow.api.messaging.dto.ChannelAccountResponse;
import com.relayflow.api.messaging.dto.ContactDetailResponse;
import com.relayflow.api.messaging.dto.ContactResponse;
import com.relayflow.api.messaging.dto.ConversationResponse;
import com.relayflow.api.messaging.dto.CreateChannelAccountRequest;
import com.relayflow.api.messaging.dto.CreateContactRequest;
import com.relayflow.api.messaging.dto.CreateConversationRequest;
import com.relayflow.api.messaging.dto.CreateExternalIdentityRequest;
import com.relayflow.api.messaging.dto.CreateMessageRequest;
import com.relayflow.api.messaging.dto.CreateWorkspaceRequest;
import com.relayflow.api.messaging.dto.ExternalIdentityResponse;
import com.relayflow.api.messaging.dto.InviteMemberRequest;
import com.relayflow.api.messaging.dto.MergeContactRequest;
import com.relayflow.api.messaging.dto.MessageResponse;
import com.relayflow.api.messaging.dto.PageResponse;
import com.relayflow.api.messaging.dto.UpdateAssigneeRequest;
import com.relayflow.api.messaging.dto.UpdateConversationRequest;
import com.relayflow.api.messaging.dto.UpdateMemberRequest;
import com.relayflow.api.messaging.dto.UpdateWorkspaceRequest;
import com.relayflow.api.messaging.dto.WorkspaceMemberResponse;
import com.relayflow.api.messaging.dto.WorkspaceResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
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
public class MessagingController {

    private final MessagingService messagingService;
    private final SecurityUtils securityUtils;
    private final WorkspaceAuthorizationService authorizationService;

    public MessagingController(
            MessagingService messagingService,
            SecurityUtils securityUtils,
            WorkspaceAuthorizationService authorizationService) {
        this.messagingService = messagingService;
        this.securityUtils = securityUtils;
        this.authorizationService = authorizationService;
    }

    // --- Workspaces ---

    @GetMapping("/workspaces")
    List<WorkspaceResponse> listWorkspaces(Authentication authentication) {
        UUID userId = securityUtils.resolveUserId(authentication);

        return messagingService.listWorkspaces(userId);
    }

    @PostMapping("/workspaces")
    @ResponseStatus(HttpStatus.CREATED)
    WorkspaceResponse createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request, Authentication authentication) {
        UUID userId = securityUtils.resolveUserId(authentication);

        if (securityUtils.isAnonymous(authentication)) {
            return messagingService.createGuestWorkspace(request, userId);
        }

        return messagingService.createWorkspace(request, userId);
    }

    @PatchMapping("/workspaces/{workspaceId}")
    WorkspaceResponse updateWorkspace(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        return messagingService.updateWorkspace(workspaceId, request.name());
    }

    // --- Workspace Members ---

    @GetMapping("/workspaces/{workspaceId}/members")
    List<WorkspaceMemberResponse> listMembers(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertMember(workspaceId, authentication);

        return messagingService.listWorkspaceMembers(workspaceId);
    }

    @PostMapping("/workspaces/{workspaceId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    WorkspaceMemberResponse inviteMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody InviteMemberRequest request,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        return messagingService.inviteWorkspaceMember(
                workspaceId, request.email(), request.permissions());
    }

    @PatchMapping("/workspaces/{workspaceId}/members/{memberId}")
    WorkspaceMemberResponse updateMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRequest request,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        return messagingService.updateWorkspaceMember(
                workspaceId, memberId, request.role(), request.permissions());
    }

    @DeleteMapping("/workspaces/{workspaceId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        messagingService.removeWorkspaceMember(workspaceId, memberId);
    }

    @PutMapping("/workspaces/{workspaceId}/owner")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void transferOwnership(
            @PathVariable UUID workspaceId,
            @RequestParam UUID memberId,
            Authentication authentication) {
        UUID callerId = authorizationService.getUser(authentication);

        messagingService.transferOwnership(workspaceId, memberId, callerId);
    }

    // --- Channel Accounts ---

    @GetMapping("/channel-accounts")
    List<ChannelAccountResponse> listChannelAccounts(@RequestParam @NotNull UUID workspaceId) {
        return messagingService.listChannelAccounts(workspaceId);
    }

    @PostMapping("/channel-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    ChannelAccountResponse createChannelAccount(
            @Valid @RequestBody CreateChannelAccountRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                request.workspaceId(), authentication, WorkspacePermission.CHANNELS_WRITE);

        return messagingService.createChannelAccount(request);
    }

    @DeleteMapping("/channel-accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void disconnectChannelAccount(
            @PathVariable UUID id,
            @RequestParam @NotNull UUID workspaceId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.CHANNELS_DELETE);

        messagingService.disconnectChannelAccount(id, workspaceId);
    }

    @PostMapping("/channel-accounts/{id}/reconnect")
    ChannelAccountResponse reconnectChannelAccount(
            @PathVariable UUID id,
            @RequestParam @NotNull UUID workspaceId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.CHANNELS_WRITE);

        return messagingService.reconnectChannelAccount(id, workspaceId);
    }

    // --- Contacts ---

    @GetMapping("/contacts")
    PageResponse<ContactResponse> listContacts(
            @RequestParam @NotNull UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return messagingService.listContacts(workspaceId, page, size);
    }

    @PostMapping("/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    ContactResponse createContact(@Valid @RequestBody CreateContactRequest request) {
        return messagingService.createContact(request);
    }

    @GetMapping("/contacts/{id}")
    ContactDetailResponse getContact(
            @PathVariable UUID id, @RequestParam @NotNull UUID workspaceId) {
        return messagingService.getContactDetail(id, workspaceId);
    }

    @PostMapping("/contacts/{id}/merge")
    ContactResponse mergeContacts(
            @PathVariable UUID id,
            @RequestParam @NotNull UUID workspaceId,
            @Valid @RequestBody MergeContactRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.CONTACTS_DELETE);

        return messagingService.mergeContacts(id, request.sourceContactId(), workspaceId);
    }

    @DeleteMapping("/contacts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteContact(
            @PathVariable UUID id,
            @RequestParam @NotNull UUID workspaceId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.CONTACTS_DELETE);

        messagingService.deleteContact(id, workspaceId);
    }

    // --- External Identities ---

    @PostMapping("/external-identities")
    @ResponseStatus(HttpStatus.CREATED)
    ExternalIdentityResponse createExternalIdentity(
            @Valid @RequestBody CreateExternalIdentityRequest request) {
        return messagingService.createExternalIdentity(request);
    }

    // --- Conversations ---

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    ConversationResponse createConversation(@Valid @RequestBody CreateConversationRequest request) {
        return messagingService.createConversation(request);
    }

    @GetMapping("/conversations")
    PageResponse<ConversationResponse> listConversations(
            @RequestParam @NotNull UUID workspaceId,
            @RequestParam(required = false) UUID contactId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return messagingService.listConversations(workspaceId, contactId, null, page, size);
    }

    @GetMapping("/conversations/{conversationId}")
    ConversationResponse getConversation(
            @RequestParam @NotNull UUID workspaceId, @PathVariable UUID conversationId) {
        return messagingService.getConversation(workspaceId, conversationId);
    }

    @PatchMapping("/conversations/{conversationId}")
    ConversationResponse updateConversation(
            @RequestParam @NotNull UUID workspaceId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateConversationRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.INBOX);

        return messagingService.updateConversationStatus(
                workspaceId, conversationId, request.status());
    }

    @PatchMapping("/conversations/{conversationId}/assignee")
    ConversationResponse updateAssignee(
            @RequestParam @NotNull UUID workspaceId,
            @PathVariable UUID conversationId,
            @RequestBody UpdateAssigneeRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.INBOX);

        return messagingService.updateConversationAssignee(
                workspaceId, conversationId, request.assigneeId());
    }

    // --- Messages ---

    @GetMapping("/conversations/{conversationId}/messages")
    PageResponse<MessageResponse> listMessages(
            @RequestParam @NotNull UUID workspaceId,
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "50") int limit) {
        return messagingService.listMessages(workspaceId, conversationId, before, limit);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    MessageResponse createMessage(
            @RequestParam @NotNull UUID workspaceId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody CreateMessageRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.INBOX);

        return messagingService.createMessage(
                workspaceId, conversationId, request, authorizationService.getUser(authentication));
    }
}
