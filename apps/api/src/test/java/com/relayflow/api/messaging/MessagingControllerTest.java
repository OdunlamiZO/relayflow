package com.relayflow.api.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.channel.domain.ChannelProvider;
import com.relayflow.api.common.ResourceNotFoundException;
import com.relayflow.api.common.dto.PageResponse;
import com.relayflow.api.messaging.domain.ConversationStatus;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.dto.ConversationResponse;
import com.relayflow.api.messaging.dto.CreateConversationRequest;
import com.relayflow.api.messaging.dto.CreateMessageRequest;
import com.relayflow.api.messaging.dto.MessageResponse;
import com.relayflow.api.workspace.WorkspaceAuthorizationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MessagingController.class)
@AutoConfigureMockMvc(addFilters = false)
class MessagingControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private MessagingService messagingService;

    @MockBean private WorkspaceAuthorizationService authorizationService;

    // ── Conversations ─────────────────────────────────────────────────────────

    @Test
    void listsConversationsForWorkspace() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        UUID channelAccountId = UUID.randomUUID();
        when(messagingService.listConversations(
                        eq(workspaceId), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(
                        new PageResponse<>(
                                List.of(
                                        new ConversationResponse(
                                                conversationId,
                                                workspaceId,
                                                contactId,
                                                "Ada",
                                                channelAccountId,
                                                ChannelProvider.TELEGRAM,
                                                "Telegram Bot",
                                                ConversationStatus.OPEN,
                                                false,
                                                false,
                                                null,
                                                null,
                                                null,
                                                null,
                                                Instant.parse("2026-05-26T10:00:00Z"))),
                                false,
                                null));

        mockMvc.perform(get("/conversations").param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(conversationId.toString()))
                .andExpect(jsonPath("$.items[0].contactDisplayName").value("Ada"))
                .andExpect(jsonPath("$.items[0].channelProvider").value("TELEGRAM"))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void getConversation() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        UUID channelAccountId = UUID.randomUUID();
        when(messagingService.getConversation(workspaceId, conversationId))
                .thenReturn(
                        new ConversationResponse(
                                conversationId,
                                workspaceId,
                                contactId,
                                "Ben",
                                channelAccountId,
                                ChannelProvider.TELEGRAM,
                                "Telegram Bot",
                                ConversationStatus.OPEN,
                                false,
                                false,
                                null,
                                null,
                                null,
                                null,
                                Instant.parse("2026-05-26T10:00:00Z")));

        mockMvc.perform(
                        get("/conversations/{conversationId}", conversationId)
                                .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.contactDisplayName").value("Ben"));
    }

    @Test
    void getConversationReturns404WhenNotFound() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(messagingService.getConversation(workspaceId, conversationId))
                .thenThrow(new ResourceNotFoundException("Conversation not found"));

        mockMvc.perform(
                        get("/conversations/{conversationId}", conversationId)
                                .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Conversation not found"));
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    @Test
    void listMessages() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        when(messagingService.listMessages(eq(workspaceId), eq(conversationId), isNull(), anyInt()))
                .thenReturn(
                        new PageResponse<>(
                                List.of(
                                        new MessageResponse(
                                                messageId,
                                                workspaceId,
                                                conversationId,
                                                MessageDirection.INBOUND,
                                                MessageSenderType.CONTACT,
                                                "Hello",
                                                null,
                                                Map.of(),
                                                Instant.parse("2026-05-26T10:00:00Z"))),
                                false,
                                null));

        mockMvc.perform(
                        get("/conversations/{conversationId}/messages", conversationId)
                                .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(messageId.toString()))
                .andExpect(jsonPath("$.items[0].text").value("Hello"))
                .andExpect(jsonPath("$.items[0].direction").value("INBOUND"))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void createsMessageInConversation() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        CreateMessageRequest request =
                new CreateMessageRequest(
                        MessageDirection.INBOUND,
                        MessageSenderType.CONTACT,
                        "Hello",
                        "telegram-1",
                        Map.of("source", "test"));
        when(messagingService.createMessage(
                        eq(workspaceId),
                        eq(conversationId),
                        any(CreateMessageRequest.class),
                        any()))
                .thenReturn(
                        new MessageResponse(
                                messageId,
                                workspaceId,
                                conversationId,
                                MessageDirection.INBOUND,
                                MessageSenderType.CONTACT,
                                "Hello",
                                "telegram-1",
                                Map.of("source", "test"),
                                Instant.parse("2026-05-26T10:00:00Z")));

        mockMvc.perform(
                        post("/conversations/{conversationId}/messages", conversationId)
                                .param("workspaceId", workspaceId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(messageId.toString()))
                .andExpect(jsonPath("$.text").value("Hello"))
                .andExpect(jsonPath("$.rawPayload.source").value("test"));

        verify(messagingService)
                .createMessage(
                        eq(workspaceId),
                        eq(conversationId),
                        any(CreateMessageRequest.class),
                        any());
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void rejectsInvalidConversationRequest() throws Exception {
        CreateConversationRequest request =
                new CreateConversationRequest(null, null, null, null, null);

        mockMvc.perform(
                        post("/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresWorkspaceIdForConversations() throws Exception {
        mockMvc.perform(get("/conversations")).andExpect(status().isBadRequest());
    }
}
