package io.odunlamizo.relayflow.agent;

// Docs: docs/codebase-components.md#conversationagentworkflowcontroller

import io.odunlamizo.relayflow.agent.domain.WorkflowMapping;
import io.odunlamizo.relayflow.workspace.WorkspaceAuthorizationService;
import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workspaces/{workspaceId}/conversations/{conversationId}/agent-workflows")
public class ConversationAgentWorkflowController {

    private final ManualWorkflowTriggerService manualWorkflowTriggerService;

    private final WorkspaceAuthorizationService authorizationService;

    public ConversationAgentWorkflowController(
            ManualWorkflowTriggerService manualWorkflowTriggerService,
            WorkspaceAuthorizationService authorizationService) {
        this.manualWorkflowTriggerService = manualWorkflowTriggerService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    List<WorkflowMapping> listWorkflows(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.INBOX);

        return manualWorkflowTriggerService.listWorkflows(workspaceId, conversationId);
    }

    @PostMapping("/{workflowId}/run")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void runWorkflow(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId,
            @PathVariable UUID workflowId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.INBOX);

        manualWorkflowTriggerService.runWorkflow(workspaceId, conversationId, workflowId);
    }
}
