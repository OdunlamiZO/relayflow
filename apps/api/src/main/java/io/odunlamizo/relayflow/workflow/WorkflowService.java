package io.odunlamizo.relayflow.workflow;

// Docs: docs/codebase-components.md#workflowservice

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.common.ResourceNotFoundException;
import io.odunlamizo.relayflow.common.dto.PageResponse;
import io.odunlamizo.relayflow.hook.HookService;
import io.odunlamizo.relayflow.workflow.domain.WorkflowDefinition;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRun;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRunStep;
import io.odunlamizo.relayflow.workflow.dto.CreateWorkflowDefinitionRequest;
import io.odunlamizo.relayflow.workflow.dto.UpdateWorkflowDefinitionRequest;
import io.odunlamizo.relayflow.workflow.dto.WorkflowDefinitionResponse;
import io.odunlamizo.relayflow.workflow.dto.WorkflowRunDetailResponse;
import io.odunlamizo.relayflow.workflow.dto.WorkflowRunResponse;
import io.odunlamizo.relayflow.workflow.dto.WorkflowRunStepResponse;
import io.odunlamizo.relayflow.workflow.repository.WorkflowDefinitionRepository;
import io.odunlamizo.relayflow.workflow.repository.WorkflowRunRepository;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkflowDefinitionRepository workflowRepository;

    private final WorkspaceRepository workspaceRepository;

    private final WorkflowGraphValidator graphValidator;

    private final WorkflowRunRepository workflowRunRepository;

    private final HookService hookService;

    public WorkflowService(
            WorkflowDefinitionRepository workflowRepository,
            WorkspaceRepository workspaceRepository,
            WorkflowGraphValidator graphValidator,
            WorkflowRunRepository workflowRunRepository,
            HookService hookService) {
        this.workflowRepository = workflowRepository;
        this.workspaceRepository = workspaceRepository;
        this.graphValidator = graphValidator;
        this.workflowRunRepository = workflowRunRepository;
        this.hookService = hookService;
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
                "Workflow created {} {}",
                kv("workspaceId", workspace.getId()),
                kv("workflowId", workflow.getId()));

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

        if (Boolean.TRUE.equals(request.enabled())) {
            Map<String, Object> graphToValidate =
                    workflow.getDraftGraph() != null ? workflow.getDraftGraph() : Map.of();
            graphValidator.validate(graphToValidate);
            hookService.validateHookReferences(workspaceId, graphToValidate);
        }

        if (request.enabled() != null) {
            boolean wasEnabled = workflow.isEnabled();

            workflow.setEnabled(request.enabled());

            if (!wasEnabled && request.enabled()) {
                log.info(
                        "Workflow published {} {}",
                        kv("workspaceId", workflow.getWorkspace().getId()),
                        kv("workflowId", workflow.getId()));
            } else if (wasEnabled && !request.enabled()) {
                log.info(
                        "Workflow unpublished {} {}",
                        kv("workspaceId", workflow.getWorkspace().getId()),
                        kv("workflowId", workflow.getId()));
            }
        }

        workflowRepository.save(workflow);

        return toDto(workflow);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkflowRunResponse> listRuns(
            UUID workflowId, UUID workspaceId, int page, int size) {
        getDefinition(workflowId, workspaceId);

        int pageSize = Math.clamp(size, 1, 100);

        List<WorkflowRun> results =
                workflowRunRepository.findByWorkflowAndWorkspace(
                        workflowId, workspaceId, PageRequest.of(page, pageSize + 1));

        boolean hasMore = results.size() > pageSize;
        List<WorkflowRun> items = hasMore ? results.subList(0, pageSize) : results;

        return new PageResponse<>(items.stream().map(this::toRunDto).toList(), hasMore, null);
    }

    @Transactional(readOnly = true)
    public WorkflowRunDetailResponse getRun(UUID workflowId, UUID runId, UUID workspaceId) {
        getDefinition(workflowId, workspaceId);

        WorkflowRun run =
                workflowRunRepository
                        .findRunWithSteps(runId, workflowId, workspaceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Workflow run not found"));

        List<WorkflowRunStepResponse> steps =
                run.getSteps().stream()
                        .sorted(Comparator.comparing(WorkflowRunStep::getStartedAt))
                        .map(this::toStepDto)
                        .toList();

        return new WorkflowRunDetailResponse(
                run.getId(),
                run.getWorkflowDefinition().getId(),
                run.getConversation().getId(),
                run.getStatus(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getErrorMessage(),
                run.getWaitingAtNodeId(),
                steps);
    }

    @Transactional
    public void deleteWorkflow(UUID id, UUID workspaceId) {
        WorkflowDefinition workflow = getDefinition(id, workspaceId);

        workflowRepository.delete(workflow);

        log.info("Workflow deleted {} {}", kv("workspaceId", workspaceId), kv("workflowId", id));
    }

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

    private WorkflowDefinitionResponse toDto(WorkflowDefinition definition) {
        Map<String, Object> graph =
                definition.getDraftGraph() != null
                        ? definition.getDraftGraph()
                        : new LinkedHashMap<>();

        return new WorkflowDefinitionResponse(
                definition.getId(),
                definition.getWorkspace().getId(),
                definition.getName(),
                definition.isEnabled(),
                graph,
                definition.getCreatedAt(),
                definition.getUpdatedAt());
    }

    private WorkflowRunResponse toRunDto(WorkflowRun run) {
        return new WorkflowRunResponse(
                run.getId(),
                run.getWorkflowDefinition().getId(),
                run.getConversation().getId(),
                run.getStatus(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getErrorMessage(),
                run.getWaitingAtNodeId());
    }

    private WorkflowRunStepResponse toStepDto(WorkflowRunStep step) {
        return new WorkflowRunStepResponse(
                step.getId(),
                step.getNodeId(),
                step.getNodeType(),
                step.getStatus(),
                step.getInputSnapshot(),
                step.getOutputSnapshot(),
                step.getErrorMessage(),
                step.getStartedAt(),
                step.getFinishedAt(),
                step.getDurationMs());
    }
}
