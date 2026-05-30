package com.relayflow.api.workflow;

import com.relayflow.api.messaging.WorkspaceAuthorizationService;
import com.relayflow.api.messaging.domain.WorkspacePermission;
import com.relayflow.api.workflow.dto.CreateWorkflowDefinitionRequest;
import com.relayflow.api.workflow.dto.UpdateWorkflowDefinitionRequest;
import com.relayflow.api.workflow.dto.WorkflowDefinitionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    private final WorkspaceAuthorizationService authorizationService;

    public WorkflowController(
            WorkflowService workflowService, WorkspaceAuthorizationService authorizationService) {
        this.workflowService = workflowService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    List<WorkflowDefinitionResponse> listWorkflows(@RequestParam @NotNull UUID workspaceId) {
        return workflowService.listWorkflows(workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WorkflowDefinitionResponse createWorkflow(
            @Valid @RequestBody CreateWorkflowDefinitionRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                request.workspaceId(), authentication, WorkspacePermission.WORKFLOWS_WRITE);

        return workflowService.createWorkflow(request);
    }

    @GetMapping("/{id}")
    WorkflowDefinitionResponse getWorkflow(
            @PathVariable UUID id, @RequestParam @NotNull UUID workspaceId) {
        return workflowService.getWorkflow(id, workspaceId);
    }

    @PatchMapping("/{id}")
    WorkflowDefinitionResponse updateWorkflow(
            @PathVariable UUID id,
            @RequestParam @NotNull UUID workspaceId,
            @RequestBody UpdateWorkflowDefinitionRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WORKFLOWS_WRITE);

        return workflowService.updateWorkflow(id, workspaceId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteWorkflow(
            @PathVariable UUID id,
            @RequestParam @NotNull UUID workspaceId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WORKFLOWS_DELETE);

        workflowService.deleteWorkflow(id, workspaceId);
    }
}
