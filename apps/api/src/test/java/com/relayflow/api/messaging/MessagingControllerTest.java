package com.relayflow.api.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.messaging.domain.ChannelProvider;
import com.relayflow.api.messaging.domain.ConversationStatus;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.dto.ConversationResponse;
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

    @MockBean private SecurityUtils securityUtils;

    @Test
    void createsWorkspace() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(securityUtils.resolveUserId(any())).thenReturn(userId);
        when(messagingService.createWorkspace(any(CreateWorkspaceRequest.class), eq(userId)))
                .thenReturn(
                        new WorkspaceResponse(
                                workspaceId, "RelayFlow", Instant.parse("2026-05-26T10:00:00Z")));

        mockMvc.perform(
                        post("/api/workspaces")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new CreateWorkspaceRequest("RelayFlow"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(workspaceId.toString()))
                .andExpect(jsonPath("$.name").value("RelayFlow"));
    }

    @Test
    void listsConversationsForWorkspace() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        UUID channelAccountId = UUID.randomUUID();
        when(messagingService.listConversations(eq(workspaceId), anyInt(), anyInt()))
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
                                                null,
                                                null,
                                                Instant.parse("2026-05-26T10:00:00Z"))),
                                false,
                                null));

        mockMvc.perform(get("/api/conversations").param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(conversationId.toString()))
                .andExpect(jsonPath("$.items[0].contactDisplayName").value("Ada"))
                .andExpect(jsonPath("$.items[0].channelProvider").value("TELEGRAM"))
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
                        eq(workspaceId), eq(conversationId), any(CreateMessageRequest.class)))
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
                        post("/api/conversations/{conversationId}/messages", conversationId)
                                .param("workspaceId", workspaceId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(messageId.toString()))
                .andExpect(jsonPath("$.text").value("Hello"))
                .andExpect(jsonPath("$.rawPayload.source").value("test"));

        verify(messagingService)
                .createMessage(
                        eq(workspaceId), eq(conversationId), any(CreateMessageRequest.class));
    }

    @Test
    void rejectsInvalidConversationRequest() throws Exception {
        CreateConversationRequest request =
                new CreateConversationRequest(null, null, null, null, null);

        mockMvc.perform(
                        post("/api/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
