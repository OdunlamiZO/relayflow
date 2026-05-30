package com.relayflow.api.workflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.repository.ExternalIdentityRepository;
import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.domain.WorkflowDefinition;
import com.relayflow.api.workflow.domain.WorkflowRun;
import com.relayflow.api.workflow.domain.WorkflowRunStatus;
import com.relayflow.api.workflow.domain.WorkflowRunStep;
import com.relayflow.api.workflow.domain.WorkflowRunStepStatus;
import com.relayflow.api.workflow.repository.WorkflowRunRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core workflow execution engine.
 *
 * <p>Parses the {@code draftGraph}, initialises the execution context from conversation data, and
 * walks the node graph — executing each node and following the appropriate outgoing edge.
 *
 * <p>When a node returns {@link NodeExecutionResult#waiting()}, the run is persisted with status
 * {@link WorkflowRunStatus#WAITING} and the current context snapshot is saved. Execution resumes
 * via {@link #resumeWorkflow} when the contact replies.
 */
@Service
public class WorkflowEngineService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngineService.class);

    /** Maximum nodes to execute in a single run — prevents runaway loops. */
    private static final int MAX_STEPS = 100;

    private final WorkflowRunRepository runRepository;

    private final ExternalIdentityRepository externalIdentityRepository;

    private final ObjectMapper objectMapper;

    private final Map<NodeType, NodeExecutor> executors;

    public WorkflowEngineService(
            WorkflowRunRepository runRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ObjectMapper objectMapper,
            List<NodeExecutor> executorList) {
        this.runRepository = runRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.objectMapper = objectMapper;
        this.executors =
                executorList.stream()
                        .collect(Collectors.toMap(NodeExecutor::nodeType, Function.identity()));
    }

    /**
     * Executes a workflow definition triggered by a conversation-opened event.
     *
     * <p>{@code @Async} dispatches each call to the shared task executor so multiple workflows for
     * the same conversation run concurrently rather than sequentially. {@code REQUIRES_NEW} ensures
     * each run gets its own transaction — a failure in one workflow never affects another.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeWorkflow(
            WorkflowDefinition definition, Conversation conversation, Message triggeringMessage) {
        log.info(
                "Starting workflow run: workflow={}, conversation={}",
                definition.getId(),
                conversation.getId());

        // 1. Parse graph
        List<GraphNode> nodes = parseNodes(definition.getDraftGraph());
        List<GraphEdge> edges = parseEdges(definition.getDraftGraph());

        GraphNode triggerNode =
                nodes.stream()
                        .filter(n -> NodeType.TRIGGER.getValue().equals(n.type()))
                        .findFirst()
                        .orElse(null);

        if (triggerNode == null) {
            log.warn("Workflow {} has no trigger node — skipping", definition.getId());
            return;
        }

        // 2. Build adjacency map: nodeId → outgoing edges
        Map<String, List<GraphEdge>> adjacency = buildAdjacency(edges);

        // 3. Build node lookup map
        Map<String, GraphNode> nodeMap =
                nodes.stream().collect(Collectors.toMap(GraphNode::id, Function.identity()));

        // 4. Create the run record
        WorkflowRun run = new WorkflowRun();
        run.setWorkflowDefinition(definition);
        run.setWorkspace(conversation.getWorkspace());
        run.setConversation(conversation);
        run.setStatus(WorkflowRunStatus.RUNNING);
        runRepository.save(run);

        // 5. Initialise execution context with conversation data
        ExecutionContext context = buildContext(run, conversation, triggeringMessage);

        // 6. Walk the graph starting from the trigger node
        try {
            walk(triggerNode, nodeMap, adjacency, context, run);

            // Only mark COMPLETED if the run is still RUNNING — a WAITING run must not be
            // overwritten here; it will be completed by resumeWorkflow after the contact replies.
            if (run.getStatus() == WorkflowRunStatus.RUNNING) {
                run.setStatus(WorkflowRunStatus.COMPLETED);
                run.setFinishedAt(Instant.now());
            }
            runRepository.save(run);

            log.info(
                    "Workflow run {}: runId={}",
                    run.getStatus().toString().toLowerCase(),
                    run.getId());
        } catch (Exception e) {
            run.setStatus(WorkflowRunStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setErrorMessage(e.getMessage());
            runRepository.save(run);

            log.error("Workflow run failed: runId={}, error={}", run.getId(), e.getMessage(), e);
        }
    }

    /**
     * Resumes a workflow run that was paused at a "Wait for Reply" node.
     *
     * <p>Called by {@link WorkflowResumeListener} after the contact sends a reply. The run is
     * reloaded in a fresh transaction, the reply is applied to the execution context, and walking
     * continues from the next node after the waiting node.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resumeWorkflow(UUID runId, Message replyMessage) {
        log.info("Resuming workflow run: runId={}", runId);

        WorkflowRun run = runRepository.findWithDefinitionById(runId).orElse(null);

        if (run == null) {
            log.error("Cannot resume: workflow run {} not found", runId);
            return;
        }

        if (run.getStatus() != WorkflowRunStatus.WAITING) {
            log.warn(
                    "Workflow run {} is not WAITING (status={}) — skipping resume",
                    runId,
                    run.getStatus());
            return;
        }

        String waitingNodeId = run.getWaitingAtNodeId();

        if (waitingNodeId == null) {
            log.error("Workflow run {} is WAITING but has no waitingAtNodeId", runId);
            run.setStatus(WorkflowRunStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setErrorMessage("Run was WAITING with no waitingAtNodeId");
            runRepository.save(run);
            return;
        }

        // Parse graph
        List<GraphNode> nodes = parseNodes(run.getWorkflowDefinition().getDraftGraph());
        List<GraphEdge> edges = parseEdges(run.getWorkflowDefinition().getDraftGraph());
        Map<String, GraphNode> nodeMap =
                nodes.stream().collect(Collectors.toMap(GraphNode::id, Function.identity()));
        Map<String, List<GraphEdge>> adjacency = buildAdjacency(edges);

        GraphNode waitingNode = nodeMap.get(waitingNodeId);

        if (waitingNode == null) {
            log.error("Waiting node {} not found in graph for run {}", waitingNodeId, runId);
            run.setStatus(WorkflowRunStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setErrorMessage("Waiting node not found: " + waitingNodeId);
            runRepository.save(run);
            return;
        }

        // Rebuild execution context from the saved snapshot
        Map<String, Object> snapshot =
                run.getContextSnapshot() != null ? run.getContextSnapshot() : Map.of();
        ExecutionContext context =
                new ExecutionContext(
                        run.getId(),
                        run.getConversation().getId(),
                        run.getWorkspace().getId(),
                        snapshot);

        // Apply the contact's reply to the context and determine the next handle
        String replyText = replyMessage.getText() != null ? replyMessage.getText() : "";
        String nextHandle = applyReply(waitingNode, context, replyText);

        // Clear waiting state and resume
        run.setStatus(WorkflowRunStatus.RUNNING);
        run.setWaitingAtNodeId(null);
        run.setContextSnapshot(null);

        GraphNode nextNode = resolveNextNode(waitingNode, nextHandle, adjacency, nodeMap);

        try {
            if (nextNode != null) {
                walk(nextNode, nodeMap, adjacency, context, run);
            }

            if (run.getStatus() == WorkflowRunStatus.RUNNING) {
                run.setStatus(WorkflowRunStatus.COMPLETED);
                run.setFinishedAt(Instant.now());
            }
            runRepository.save(run);

            log.info(
                    "Resumed workflow run {}: runId={}",
                    run.getStatus().toString().toLowerCase(),
                    runId);
        } catch (Exception e) {
            run.setStatus(WorkflowRunStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setErrorMessage(e.getMessage());
            runRepository.save(run);

            log.error("Resumed workflow run failed: runId={}, error={}", runId, e.getMessage(), e);
        }
    }

    // ── graph walking ──────────────────────────────────────────────────────────

    private void walk(
            GraphNode startNode,
            Map<String, GraphNode> nodeMap,
            Map<String, List<GraphEdge>> adjacency,
            ExecutionContext context,
            WorkflowRun run) {
        GraphNode current = startNode;
        int stepCount = 0;

        while (current != null) {
            if (stepCount++ >= MAX_STEPS) {
                throw new IllegalStateException(
                        "Workflow exceeded maximum step limit of " + MAX_STEPS);
            }

            current = executeStep(current, nodeMap, adjacency, context, run);
        }
    }

    /**
     * Executes a single node, records the step, and returns the next node to execute (or null if
     * the run should end or pause).
     */
    private GraphNode executeStep(
            GraphNode node,
            Map<String, GraphNode> nodeMap,
            Map<String, List<GraphEdge>> adjacency,
            ExecutionContext context,
            WorkflowRun run) {
        NodeType nodeType = NodeType.fromValue(node.type()).orElse(null);
        NodeExecutor executor = nodeType != null ? executors.get(nodeType) : null;

        if (executor == null) {
            log.warn("No executor found for node type '{}' — skipping", node.type());
            return null;
        }

        WorkflowRunStep step = newStep(node, run, context);
        Instant stepStart = Instant.now();

        try {
            NodeExecutionResult result = executor.execute(node, context);

            finishStep(step, WorkflowRunStepStatus.COMPLETED, result.output(), stepStart, null);
            run.getSteps().add(step);

            log.debug(
                    "Node executed: runId={}, nodeId={}, type={}",
                    run.getId(),
                    node.id(),
                    node.type());

            if (result.waiting()) {
                // Pause the run — resumeWorkflow will continue after the contact replies.
                run.setStatus(WorkflowRunStatus.WAITING);
                run.setWaitingAtNodeId(node.id());
                run.setContextSnapshot(context.snapshot());

                log.debug(
                        "Workflow run paused waiting for reply: runId={}, nodeId={}",
                        run.getId(),
                        node.id());

                return null;
            }

            if (result.jumpToNodeId() != null) {
                GraphNode jumpTarget = nodeMap.get(result.jumpToNodeId());

                if (jumpTarget == null) {
                    throw new NodeExecutionException(
                            "Jump target node '"
                                    + result.jumpToNodeId()
                                    + "' not found in workflow graph");
                }

                log.debug(
                        "Jump taken: runId={}, from={}, to={}",
                        run.getId(),
                        node.id(),
                        result.jumpToNodeId());

                return jumpTarget;
            }

            return resolveNextNode(node, result.nextHandle(), adjacency, nodeMap);

        } catch (Exception e) {
            finishStep(step, WorkflowRunStepStatus.FAILED, Map.of(), stepStart, e.getMessage());
            run.getSteps().add(step);

            throw e;
        }
    }

    private GraphNode resolveNextNode(
            GraphNode current,
            String nextHandle,
            Map<String, List<GraphEdge>> adjacency,
            Map<String, GraphNode> nodeMap) {
        List<GraphEdge> outgoing = adjacency.getOrDefault(current.id(), List.of());

        Optional<GraphEdge> match;

        if (nextHandle == null) {
            // Single-output node — follow the edge with no sourceHandle
            match =
                    outgoing.stream()
                            .filter(e -> e.sourceHandle() == null || e.sourceHandle().isBlank())
                            .findFirst()
                            .or(() -> outgoing.stream().findFirst()); // fallback: any edge
        } else {
            match = outgoing.stream().filter(e -> nextHandle.equals(e.sourceHandle())).findFirst();
        }

        return match.map(e -> nodeMap.get(e.target())).orElse(null);
    }

    // ── reply handling ─────────────────────────────────────────────────────────

    /**
     * Applies the contact's reply to the execution context and returns the {@code nextHandle} to
     * use for routing (or {@code null} for the default single-output edge in generic mode).
     */
    @SuppressWarnings("unchecked")
    private String applyReply(GraphNode waitingNode, ExecutionContext context, String replyText) {
        String responseType = (String) waitingNode.data().getOrDefault("responseType", "generic");

        if ("generic".equals(responseType)) {
            String variableName = (String) waitingNode.data().get("responseVariable");

            if (variableName != null && !variableName.isBlank()) {
                context.setVariable(variableName.trim(), replyText);
            }

            return null; // single default edge

        } else if ("defined".equals(responseType)) {
            List<Map<String, Object>> options =
                    (List<Map<String, Object>>)
                            waitingNode.data().getOrDefault("options", List.of());
            String lowerReply = replyText.trim().toLowerCase();

            for (Map<String, Object> option : options) {
                String text = (String) option.get("text");

                if (text != null && text.trim().equalsIgnoreCase(lowerReply)) {
                    return (String) option.get("id");
                }
            }

            return "default"; // no option matched → follow the "Other" edge
        }

        return null;
    }

    // ── context initialisation ─────────────────────────────────────────────────

    private ExecutionContext buildContext(
            WorkflowRun run, Conversation conversation, Message triggeringMessage) {
        Map<String, Object> vars = new LinkedHashMap<>();

        vars.put("workspace.id", conversation.getWorkspace().getId().toString());
        vars.put("conversation.id", conversation.getId().toString());
        vars.put("conversation.channel", conversation.getChannelAccount().getProvider().name());
        vars.put("contact.id", conversation.getContact().getId().toString());
        vars.put("contact.name", nullSafe(conversation.getContact().getDisplayName()));

        // Contact identifier from external identity (e.g. Telegram username / phone)
        externalIdentityRepository
                .findForContactOnProvider(
                        conversation.getContact().getId(),
                        conversation.getChannelAccount().getProvider())
                .ifPresent(
                        identity -> {
                            vars.put("contact.identifier", nullSafe(identity.getExternalUserId()));
                            if (identity.getUsername() != null) {
                                vars.put("contact.username", identity.getUsername());
                            }
                        });

        // Populated only when the workflow is triggered by conversation_opened.
        if (triggeringMessage != null) {
            vars.put("customer.intent", nullSafe(triggeringMessage.getText()));
        }

        return new ExecutionContext(
                run.getId(), conversation.getId(), conversation.getWorkspace().getId(), vars);
    }

    // ── step helpers ───────────────────────────────────────────────────────────

    private WorkflowRunStep newStep(GraphNode node, WorkflowRun run, ExecutionContext context) {
        WorkflowRunStep step = new WorkflowRunStep();
        step.setRun(run);
        step.setNodeId(node.id());
        step.setNodeType(node.type());
        step.setStatus(WorkflowRunStepStatus.COMPLETED);
        step.setInputSnapshot(context.snapshot());

        return step;
    }

    private void finishStep(
            WorkflowRunStep step,
            WorkflowRunStepStatus status,
            Map<String, Object> output,
            Instant startedAt,
            String errorMessage) {
        Instant now = Instant.now();
        step.setStatus(status);
        step.setOutputSnapshot(output != null ? output : Map.of());
        step.setErrorMessage(errorMessage);
        step.setStartedAt(startedAt);
        step.setFinishedAt(now);
        step.setDurationMs(now.toEpochMilli() - startedAt.toEpochMilli());
    }

    // ── graph parsing ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<GraphNode> parseNodes(Map<String, Object> graph) {
        List<Map<String, Object>> rawNodes =
                (List<Map<String, Object>>) graph.getOrDefault("nodes", List.of());

        return rawNodes.stream()
                .map(
                        raw -> {
                            String id = (String) raw.get("id");
                            String type = (String) raw.get("type");
                            Map<String, Object> data =
                                    (Map<String, Object>) raw.getOrDefault("data", Map.of());

                            return new GraphNode(id, type, data);
                        })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<GraphEdge> parseEdges(Map<String, Object> graph) {
        List<Map<String, Object>> rawEdges =
                (List<Map<String, Object>>) graph.getOrDefault("edges", List.of());

        return rawEdges.stream()
                .map(
                        raw ->
                                new GraphEdge(
                                        (String) raw.get("id"),
                                        (String) raw.get("source"),
                                        (String) raw.get("target"),
                                        (String) raw.get("sourceHandle")))
                .toList();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Map<String, List<GraphEdge>> buildAdjacency(List<GraphEdge> edges) {
        Map<String, List<GraphEdge>> adjacency = new HashMap<>();

        for (GraphEdge edge : edges) {
            adjacency.computeIfAbsent(edge.source(), k -> new ArrayList<>()).add(edge);
        }

        return adjacency;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
