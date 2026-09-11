package com.relayflow.api.workspace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.workspace.dto.CreateWorkspaceRequest;
import com.relayflow.api.workspace.dto.WorkspaceResponse;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkspaceControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private WorkspaceService workspaceService;

    @MockBean private WorkspaceAuthorizationService workspaceAuthorizationService;

    @MockBean private SecurityUtils securityUtils;

    @Test
    void listWorkspaces() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        when(securityUtils.resolveUserId(any())).thenReturn(userId);
        when(workspaceService.listWorkspaces(userId))
                .thenReturn(
                        List.of(
                                new WorkspaceResponse(
                                        workspaceId,
                                        "Acme",
                                        List.of(),
                                        Instant.parse("2026-05-26T10:00:00Z"))));

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
        when(workspaceService.createWorkspace(any(CreateWorkspaceRequest.class), eq(userId)))
                .thenReturn(
                        new WorkspaceResponse(
                                workspaceId,
                                "RelayFlow",
                                List.of(),
                                Instant.parse("2026-05-26T10:00:00Z")));

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

    @Test
    void createWorkspaceReturns403WhenCallerOwnsNoWorkspace() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace owner required"))
                .when(workspaceAuthorizationService)
                .assertOwnerOfAnyWorkspace(any());

        mockMvc.perform(
                        post("/workspaces")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new CreateWorkspaceRequest("RelayFlow"))))
                .andExpect(status().isForbidden());
    }
}
