package com.relayflow.api.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.messaging.domain.ChannelAccountStatus;
import com.relayflow.api.messaging.domain.ChannelProvider;
import com.relayflow.api.messaging.domain.ConversationStatus;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.dto.ChannelAccountResponse;
import com.relayflow.api.messaging.dto.ConversationResponse;
import com.relayflow.api.messaging.dto.CreateChannelAccountRequest;
import com.relayflow.api.messaging.dto.CreateConversationRequest;
import com.relayflow.api.messaging.dto.CreateMessageRequest;
import com.relayflow.api.messaging.dto.CreateWorkspaceRequest;
import com.relayflow.api.messaging.dto.MessageResponse;
import com.relayflow.api.messaging.dto.PageResponse;
import com.relayflow.api.messaging.dto.WorkspaceResponse;
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

    @MockBean private WorkspaceAuthorizationService workspaceAuthorizationService;

    @MockBean private SecurityUtils securityUtils;

    // ── Workspaces ────────────────────────────────────────────────────────────

    @Test
    void listWorkspaces() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        when(securityUtils.resolveUserId(any())).thenReturn(userId);
        when(messagingService.listWorkspaces(userId))
                .thenReturn(
                        List.of(
                                new WorkspaceResponse(
                                        workspaceId,
                                        "Acme",
                                        Instant.parse("2026-05-26T10:00:00Z"),
                                        false)));

        mockMvc.perform(get("/workspaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(workspaceId.toString()))
                .andExpect(jsonPath("$[0].name").value("Acme"));
    }

    @Test
    void createsWorkspace() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(securityUtils.resolveUserId(any())).thenReturn(userId);
        when(messagingService.createWorkspace(any(CreateWorkspaceRequest.class), eq(userId)))
                .thenReturn(
                        new WorkspaceResponse(
                                workspaceId,
                                "RelayFlow",
                                Instant.parse("2026-05-26T10:00:00Z"),
                                false));

        mockMvc.perform(
                        post("/workspaces")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new CreateWorkspaceRequest("RelayFlow"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(workspaceId.toString()))
                .andExpect(jsonPath("$.name").value("RelayFlow"));
    }

    // ── Channel Accounts ──────────────────────────────────────────────────────

    @Test
    void listChannelAccounts() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        when(messagingService.listChannelAccounts(workspaceId))
                .thenReturn(
                        List.of(
                                new ChannelAccountResponse(
                                        channelId,
                                        workspaceId,
                                        ChannelProvider.TELEGRAM,
                                        "My Bot",
                                        ChannelAccountStatus.ACTIVE,
                                        false,
                                        Map.of(),
                                        Instant.parse("2026-05-26T10:00:00Z"))));

        mockMvc.perform(get("/channel-accounts").param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(channelId.toString()))
                .andExpect(jsonPath("$[0].name").value("My Bot"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].shared").value(false));
    }

    @Test
    void createChannelAccount() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        CreateChannelAccountRequest request =
                new CreateChannelAccountRequest(
                        workspaceId, ChannelProvider.TELEGRAM, "My Bot", null, "bot-token", null);
        when(messagingService.createChannelAccount(any(CreateChannelAccountRequest.class)))
                .thenReturn(
                        new ChannelAccountResponse(
                                channelId,
                                workspaceId,
                                ChannelProvider.TELEGRAM,
                                "My Bot",
                                ChannelAccountStatus.ACTIVE,
                                false,
                                Map.of(),
                                Instant.parse("2026-05-26T10:00:00Z")));

        mockMvc.perform(
                        post("/channel-accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(channelId.toString()))
                .andExpect(jsonPath("$.provider").value("TELEGRAM"));
    }

    @Test
    void disconnectChannelAccount() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/channel-accounts/{id}", channelId)
                                .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isNoContent());

        verify(messagingService).disconnectChannelAccount(channelId, workspaceId);
    }

    @Test
    void reconnectChannelAccount() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        when(messagingService.reconnectChannelAccount(channelId, workspaceId))
                .thenReturn(
                        new ChannelAccountResponse(
                                channelId,
                                workspaceId,
                                ChannelProvider.TELEGRAM,
                                "My Bot",
                                ChannelAccountStatus.ACTIVE,
                                false,
                                Map.of(),
                                Instant.parse("2026-05-26T10:00:00Z")));

        mockMvc.perform(
                        post("/channel-accounts/{id}/reconnect", channelId)
                                .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void disconnectChannelAccountReturns404WhenNotFound() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Channel account not found"))
                .when(messagingService)
                .disconnectChannelAccount(channelId, workspaceId);

        mockMvc.perform(
                        delete("/channel-accounts/{id}", channelId)
                                .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Channel account not found"));
    }

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
    void requiresWorkspaceIdForChannelAccounts() throws Exception {
        mockMvc.perform(get("/channel-accounts")).andExpect(status().isBadRequest());
    }

    @Test
    void requiresWorkspaceIdForConversations() throws Exception {
        mockMvc.perform(get("/conversations")).andExpect(status().isBadRequest());
    }
}
