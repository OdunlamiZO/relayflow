package com.relayflow.api.hook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.hook.domain.HookOutcomeStatus;
import com.relayflow.api.hook.dto.SaveHookRequest;
import com.relayflow.api.hook.dto.TestHookRequest;
import com.relayflow.api.hook.dto.TestHookResponse;
import com.relayflow.api.workspace.WorkspaceAuthorizationService;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(HookController.class)
@AutoConfigureMockMvc(addFilters = false)
class HookControllerTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private HookService hookService;

    @MockBean private WorkspaceAuthorizationService authorizationService;

    @MockBean private SecurityUtils securityUtils;

    @Test
    void testEndpointReturnsTheOutcome() throws Exception {
        when(hookService.testHook(any()))
                .thenReturn(
                        new TestHookResponse(
                                HookOutcomeStatus.ACCEPTED,
                                "ada@example.com",
                                null,
                                Map.of(),
                                List.of()));

        mockMvc.perform(
                        post("/workspaces/{workspaceId}/hooks/test", WORKSPACE_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new TestHookRequest(
                                                        "lower case(value)",
                                                        "Ada@Example.com",
                                                        null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.value").value("ada@example.com"));

        verify(authorizationService)
                .assertPermission(eq(WORKSPACE_ID), any(), eq(WorkspacePermission.WORKFLOWS_WRITE));
    }

    @Test
    void createRejectsABlankExpression() throws Exception {
        mockMvc.perform(
                        post("/workspaces/{workspaceId}/hooks", WORKSPACE_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new SaveHookRequest(
                                                        "Order ID", null, " ", "Bad order ID"))))
                .andExpect(status().isBadRequest());

        verify(hookService, never()).createHook(any(), any());
    }

    @Test
    void createWithoutPermissionIsForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(authorizationService)
                .assertPermission(eq(WORKSPACE_ID), any(), eq(WorkspacePermission.WORKFLOWS_WRITE));

        mockMvc.perform(
                        post("/workspaces/{workspaceId}/hooks", WORKSPACE_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new SaveHookRequest(
                                                        "Order ID",
                                                        null,
                                                        "matches(value, \"^ORD\")",
                                                        "Bad order ID"))))
                .andExpect(status().isForbidden());

        verify(hookService, never()).createHook(any(), any());
    }
}
