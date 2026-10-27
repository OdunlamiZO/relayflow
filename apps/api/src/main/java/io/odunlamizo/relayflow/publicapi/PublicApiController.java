package io.odunlamizo.relayflow.publicapi;

import io.odunlamizo.relayflow.authentication.ApiKeyAuthentication;
import io.odunlamizo.relayflow.common.dto.PageResponse;
import io.odunlamizo.relayflow.contact.ContactService;
import io.odunlamizo.relayflow.messaging.MessagingService;
import io.odunlamizo.relayflow.messaging.domain.MessageSenderType;
import io.odunlamizo.relayflow.messaging.dto.ConversationResponse;
import io.odunlamizo.relayflow.messaging.dto.CreateMessageRequest;
import io.odunlamizo.relayflow.messaging.dto.MessageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    private final ContactService contactService;

    public PublicApiController(MessagingService messagingService, ContactService contactService) {
        this.messagingService = messagingService;
        this.contactService = contactService;
    }

    /** Filter by contactId to find a conversation after a contact.created webhook. */
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
                        io.odunlamizo.relayflow.messaging.domain.MessageDirection.OUTBOUND,
                        MessageSenderType.AGENT,
                        request.text(),
                        null,
                        null);

        return messagingService.createMessage(
                workspaceId, conversationId, createRequest, null, false);
    }

    @PutMapping("/contacts/{contactId}/tags")
    ContactTagsResponse updateContactTags(
            @PathVariable UUID contactId,
            @RequestBody Map<String, String> tags,
            Authentication authentication) {
        UUID workspaceId = resolveWorkspace(authentication);

        return new ContactTagsResponse(
                contactId, contactService.updateContactTags(contactId, workspaceId, tags).tags());
    }

    public record ContactTagsResponse(UUID contactId, Map<String, String> tags) {}

    public record SendMessageRequest(@NotBlank String text) {}

    private UUID resolveWorkspace(Authentication authentication) {
        if (authentication instanceof ApiKeyAuthentication apiKeyAuthentication) {
            return apiKeyAuthentication.getWorkspaceId();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
    }
}
