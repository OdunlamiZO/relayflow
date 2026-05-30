package com.relayflow.api.workflow;

import com.relayflow.api.messaging.ResourceNotFoundException;
import com.relayflow.api.messaging.domain.Workspace;
import com.relayflow.api.messaging.repository.WorkspaceRepository;
import com.relayflow.api.workflow.domain.WorkflowDefinition;
import com.relayflow.api.workflow.dto.CreateWorkflowDefinitionRequest;
import com.relayflow.api.workflow.dto.UpdateWorkflowDefinitionRequest;
import com.relayflow.api.workflow.dto.WorkflowDefinitionResponse;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkflowDefinitionRepository workflowRepository;

    private final WorkspaceRepository workspaceRepository;

    private final WorkflowGraphValidator graphValidator;

    public WorkflowService(
            WorkflowDefinitionRepository workflowRepository,
            WorkspaceRepository workspaceRepository,
            WorkflowGraphValidator graphValidator) {
        this.workflowRepository = workflowRepository;
        this.workspaceRepository = workspaceRepository;
        this.graphValidator = graphValidator;
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponse> listWorkflows(UUID workspaceId) {
        getWorkspace(workspaceId);

        return workflowRepository.findByWorkspace(workspaceId).stream().map(this::toDto).toList();
    }

    @Transactional
    public WorkflowDefinitionResponse createWorkflow(CreateWorkflowDefinitionRequest request) {
        Workspace workspace = getWorkspace(request.workspaceId());

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setWorkspace(workspace);
        workflow.setName(request.name());

        workflowRepository.save(workflow);

        log.info(
                "Workflow created: id={}, workspace={}, name={}",
                workflow.getId(),
                workspace.getId(),
                workflow.getName());

        return toDto(workflow);
    }

    @Transactional(readOnly = true)
    public WorkflowDefinitionResponse getWorkflow(UUID id, UUID workspaceId) {
        return toDto(getDefinition(id, workspaceId));
    }

    @Transactional
    public WorkflowDefinitionResponse updateWorkflow(
            UUID id, UUID workspaceId, UpdateWorkflowDefinitionRequest request) {
        WorkflowDefinition workflow = getDefinition(id, workspaceId);

        if (request.name() != null && !request.name().isBlank()) {
            workflow.setName(request.name());
        }

        if (request.draftGraph() != null) {
            workflow.setDraftGraph(new LinkedHashMap<>(request.draftGraph()));
        }

        // Validate graph before enabling — saving an incomplete draft is always allowed.
        if (Boolean.TRUE.equals(request.enabled())) {
            Map<String, Object> graphToValidate =
                    workflow.getDraftGraph() != null ? workflow.getDraftGraph() : Map.of();
            graphValidator.validate(graphToValidate);
        }

        if (request.enabled() != null) {
            boolean wasEnabled = workflow.isEnabled();

            workflow.setEnabled(request.enabled());

            if (!wasEnabled && request.enabled()) {
                log.info(
                        "Workflow published: id={}, workspace={}",
                        workflow.getId(),
                        workflow.getWorkspace().getId());
            } else if (wasEnabled && !request.enabled()) {
                log.info(
                        "Workflow unpublished: id={}, workspace={}",
                        workflow.getId(),
                        workflow.getWorkspace().getId());
            }
        }

        workflowRepository.save(workflow);

        return toDto(workflow);
    }

    @Transactional
    public void deleteWorkflow(UUID id, UUID workspaceId) {
        WorkflowDefinition workflow = getDefinition(id, workspaceId);

        workflowRepository.delete(workflow);

        log.info("Workflow deleted: id={}, workspace={}", id, workspaceId);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private WorkflowDefinition getDefinition(UUID id, UUID workspaceId) {
        return workflowRepository
                .findInWorkspace(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
    }

    private Workspace getWorkspace(UUID workspaceId) {
        return workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
    }

    private WorkflowDefinitionResponse toDto(WorkflowDefinition w) {
        Map<String, Object> graph =
                w.getDraftGraph() != null ? w.getDraftGraph() : new LinkedHashMap<>();

        return new WorkflowDefinitionResponse(
                w.getId(),
                w.getWorkspace().getId(),
                w.getName(),
                w.isEnabled(),
                graph,
                w.getCreatedAt(),
                w.getUpdatedAt());
    }
}
