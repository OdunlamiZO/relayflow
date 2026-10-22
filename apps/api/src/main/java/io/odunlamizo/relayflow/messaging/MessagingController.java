package io.odunlamizo.relayflow.messaging;

import io.odunlamizo.relayflow.common.dto.PageResponse;
import io.odunlamizo.relayflow.messaging.dto.ConversationResponse;
import io.odunlamizo.relayflow.messaging.dto.CreateConversationRequest;
import io.odunlamizo.relayflow.messaging.dto.CreateMessageRequest;
import io.odunlamizo.relayflow.messaging.dto.MessageResponse;
import io.odunlamizo.relayflow.messaging.dto.UpdateAssigneeRequest;
import io.odunlamizo.relayflow.messaging.dto.UpdateConversationRequest;
import io.odunlamizo.relayflow.workspace.WorkspaceAuthorizationService;
import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class MessagingController {

    private final MessagingService messagingService;

    private final WorkspaceAuthorizationService authorizationService;

    public MessagingController(
            MessagingService messagingService, WorkspaceAuthorizationService authorizationService) {
        this.messagingService = messagingService;
        this.authorizationService = authorizationService;
    }

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
