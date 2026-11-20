package io.odunlamizo.relayflow.agent;

// Docs: docs/codebase-components.md#backend-tests

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.odunlamizo.relayflow.agent.domain.WorkflowMapping;
import io.odunlamizo.relayflow.authentication.SecurityUtils;
import io.odunlamizo.relayflow.workspace.WorkspaceAuthorizationService;
import io.odunlamizo.relayflow.workspace.WorkspaceService;
import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(ConversationAgentWorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConversationAgentWorkflowControllerTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    private static final UUID CONVERSATION_ID = UUID.randomUUID();

    private static final UUID WORKFLOW_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;

    @MockBean private ManualWorkflowTriggerService manualWorkflowTriggerService;

    @MockBean private WorkspaceService workspaceService;

    @MockBean private WorkspaceAuthorizationService authorizationService;

    @MockBean private SecurityUtils securityUtils;

    @Test
    void listsTheAgentsWorkflows() throws Exception {
        when(manualWorkflowTriggerService.listWorkflows(WORKSPACE_ID, CONVERSATION_ID))
                .thenReturn(List.of(new WorkflowMapping(WORKFLOW_ID, "Loan", "Wants a loan")));

        mockMvc.perform(
                        get(
                                "/workspaces/{workspaceId}/conversations/{conversationId}/agent-workflows",
                                WORKSPACE_ID,
                                CONVERSATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workflowId").value(WORKFLOW_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Loan"));
    }

    @Test
    void runStartsTheWorkflow() throws Exception {
        mockMvc.perform(
                        post(
                                "/workspaces/{workspaceId}/conversations/{conversationId}/agent-workflows/{workflowId}/run",
                                WORKSPACE_ID,
                                CONVERSATION_ID,
                                WORKFLOW_ID))
                .andExpect(status().isNoContent());

        verify(manualWorkflowTriggerService)
                .runWorkflow(WORKSPACE_ID, CONVERSATION_ID, WORKFLOW_ID);
    }

    @Test
    void runNeedsInboxAccess() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission required"))
                .when(authorizationService)
                .assertPermission(eq(WORKSPACE_ID), any(), eq(WorkspacePermission.INBOX));

        mockMvc.perform(
                        post(
                                "/workspaces/{workspaceId}/conversations/{conversationId}/agent-workflows/{workflowId}/run",
                                WORKSPACE_ID,
                                CONVERSATION_ID,
                                WORKFLOW_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(manualWorkflowTriggerService);
    }
}
