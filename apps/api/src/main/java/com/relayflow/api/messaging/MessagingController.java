package com.relayflow.api.messaging;

import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.messaging.dto.ChannelAccountResponse;
import com.relayflow.api.messaging.dto.ContactResponse;
import com.relayflow.api.messaging.dto.ConversationResponse;
import com.relayflow.api.messaging.dto.CreateChannelAccountRequest;
import com.relayflow.api.messaging.dto.CreateContactRequest;
import com.relayflow.api.messaging.dto.CreateConversationRequest;
import com.relayflow.api.messaging.dto.CreateExternalIdentityRequest;
import com.relayflow.api.messaging.dto.CreateMessageRequest;
import com.relayflow.api.messaging.dto.CreateWorkspaceRequest;
import com.relayflow.api.messaging.dto.ExternalIdentityResponse;
import com.relayflow.api.messaging.dto.MessageResponse;
import com.relayflow.api.messaging.dto.PageResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class MessagingController {

    private final MessagingService messagingService;
    private final SecurityUtils securityUtils;

    public MessagingController(MessagingService messagingService, SecurityUtils securityUtils) {
        this.messagingService = messagingService;
        this.securityUtils = securityUtils;
    }

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

    @GetMapping("/channel-accounts")
    List<ChannelAccountResponse> listChannelAccounts(@RequestParam @NotNull UUID workspaceId) {
        return messagingService.listChannelAccounts(workspaceId);
    }

    @PostMapping("/channel-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    ChannelAccountResponse createChannelAccount(
            @Valid @RequestBody CreateChannelAccountRequest request) {
        return messagingService.createChannelAccount(request);
    }

    @DeleteMapping("/channel-accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void disconnectChannelAccount(@PathVariable UUID id, @RequestParam @NotNull UUID workspaceId) {
        messagingService.disconnectChannelAccount(id, workspaceId);
    }

    @PostMapping("/channel-accounts/{id}/reconnect")
    ChannelAccountResponse reconnectChannelAccount(
            @PathVariable UUID id, @RequestParam @NotNull UUID workspaceId) {
        return messagingService.reconnectChannelAccount(id, workspaceId);
    }

    @PostMapping("/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    ContactResponse createContact(@Valid @RequestBody CreateContactRequest request) {
        return messagingService.createContact(request);
    }

    @PostMapping("/external-identities")
    @ResponseStatus(HttpStatus.CREATED)
    ExternalIdentityResponse createExternalIdentity(
            @Valid @RequestBody CreateExternalIdentityRequest request) {
        return messagingService.createExternalIdentity(request);
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    ConversationResponse createConversation(@Valid @RequestBody CreateConversationRequest request) {
        return messagingService.createConversation(request);
    }

    @GetMapping("/conversations")
    PageResponse<ConversationResponse> listConversations(
            @RequestParam @NotNull UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return messagingService.listConversations(workspaceId, page, size);
    }

    @GetMapping("/conversations/{conversationId}")
    ConversationResponse getConversation(
            @RequestParam @NotNull UUID workspaceId, @PathVariable UUID conversationId) {
        return messagingService.getConversation(workspaceId, conversationId);
    }

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
            @Valid @RequestBody CreateMessageRequest request) {
        return messagingService.createMessage(workspaceId, conversationId, request);
    }
}
