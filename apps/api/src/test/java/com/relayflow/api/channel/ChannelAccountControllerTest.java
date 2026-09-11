package com.relayflow.api.channel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.channel.domain.ChannelAccountStatus;
import com.relayflow.api.channel.domain.ChannelProvider;
import com.relayflow.api.channel.dto.ChannelAccountResponse;
import com.relayflow.api.channel.dto.CreateChannelAccountRequest;
import com.relayflow.api.common.ResourceNotFoundException;
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

@WebMvcTest(ChannelAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChannelAccountControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private ChannelAccountService channelAccountService;

    @MockBean private WorkspaceAuthorizationService authorizationService;

    @Test
    void listChannelAccounts() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        when(channelAccountService.listChannelAccounts(workspaceId))
                .thenReturn(
                        List.of(
                                new ChannelAccountResponse(
                                        channelId,
                                        workspaceId,
                                        ChannelProvider.TELEGRAM,
                                        "My Bot",
                                        ChannelAccountStatus.ACTIVE,
                                        Map.of(),
                                        Instant.parse("2026-05-26T10:00:00Z"))));

        mockMvc.perform(get("/channel-accounts").param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(channelId.toString()))
                .andExpect(jsonPath("$[0].name").value("My Bot"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void createChannelAccount() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        CreateChannelAccountRequest request =
                new CreateChannelAccountRequest(
                        workspaceId, ChannelProvider.TELEGRAM, "My Bot", null, "bot-token", null);
        when(channelAccountService.createChannelAccount(any(CreateChannelAccountRequest.class)))
                .thenReturn(
                        new ChannelAccountResponse(
                                channelId,
                                workspaceId,
                                ChannelProvider.TELEGRAM,
                                "My Bot",
                                ChannelAccountStatus.ACTIVE,
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

        verify(channelAccountService).disconnectChannelAccount(channelId, workspaceId);
    }

    @Test
    void reconnectChannelAccount() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        when(channelAccountService.reconnectChannelAccount(channelId, workspaceId))
                .thenReturn(
                        new ChannelAccountResponse(
                                channelId,
                                workspaceId,
                                ChannelProvider.TELEGRAM,
                                "My Bot",
                                ChannelAccountStatus.ACTIVE,
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
                .when(channelAccountService)
                .disconnectChannelAccount(channelId, workspaceId);

        mockMvc.perform(
                        delete("/channel-accounts/{id}", channelId)
                                .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Channel account not found"));
    }

    @Test
    void requiresWorkspaceIdForChannelAccounts() throws Exception {
        mockMvc.perform(get("/channel-accounts")).andExpect(status().isBadRequest());
    }
}
