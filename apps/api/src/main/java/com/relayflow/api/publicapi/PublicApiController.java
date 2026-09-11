package com.relayflow.api.publicapi;

import com.relayflow.api.authentication.ApiKeyAuthentication;
import com.relayflow.api.common.dto.PageResponse;
import com.relayflow.api.messaging.MessagingService;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.dto.ConversationResponse;
import com.relayflow.api.messaging.dto.CreateMessageRequest;
import com.relayflow.api.messaging.dto.MessageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/public/v1")
public class PublicApiController {

    private final MessagingService messagingService;

    public PublicApiController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    // --- Conversations ---

    /**
     * Lists conversations for the authenticated workspace.
     *
     * <p>Pass {@code contactId} to filter conversations to a single contact — this is the intended
     * way to find a conversation after receiving a {@code contact.created} webhook.
     */
    @GetMapping("/conversations")
    PageResponse<ConversationResponse> listConversations(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) UUID contactId,
            @RequestParam(required = false) UUID channelAccountId) {
        UUID workspaceId = resolveWorkspace(authentication);

        return messagingService.listConversations(
                workspaceId, contactId, channelAccountId, page, size);
    }

    @GetMapping("/conversations/{conversationId}")
    ConversationResponse getConversation(
            @PathVariable UUID conversationId, Authentication authentication) {
        UUID workspaceId = resolveWorkspace(authentication);

        return messagingService.getConversation(workspaceId, conversationId);
    }

    // --- Messages ---

    @GetMapping("/conversations/{conversationId}/messages")
    PageResponse<MessageResponse> listMessages(
            @PathVariable UUID conversationId,
            Authentication authentication,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "50") int limit) {
        UUID workspaceId = resolveWorkspace(authentication);

        return messagingService.listMessages(workspaceId, conversationId, before, limit);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    MessageResponse sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        UUID workspaceId = resolveWorkspace(authentication);
        CreateMessageRequest createRequest =
                new CreateMessageRequest(
                        com.relayflow.api.messaging.domain.MessageDirection.OUTBOUND,
                        MessageSenderType.AGENT,
                        request.text(),
                        null,
                        null);

        // Public API sends (including future AI-agent-driven sends) have no human workspace
        // member to auto-assign the conversation to.
        return messagingService.createMessage(workspaceId, conversationId, createRequest, null);
    }

    /** Request body for the public send-message endpoint. */
    public record SendMessageRequest(@NotBlank String text) {}

    private UUID resolveWorkspace(Authentication authentication) {
        if (authentication instanceof ApiKeyAuthentication apiKeyAuth) {
            return apiKeyAuth.getWorkspaceId();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
    }
}
