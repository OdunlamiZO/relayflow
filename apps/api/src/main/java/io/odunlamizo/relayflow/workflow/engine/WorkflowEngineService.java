package io.odunlamizo.relayflow.workflow.engine;

// Docs: docs/codebase-components.md#workflowengineservice

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.contact.repository.ExternalIdentityRepository;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.domain.Message;
import io.odunlamizo.relayflow.messaging.repository.ConversationRepository;
import io.odunlamizo.relayflow.sse.SseBroadcastEvent;
import io.odunlamizo.relayflow.sse.SseEventType;
import io.odunlamizo.relayflow.workflow.NodeType;
import io.odunlamizo.relayflow.workflow.domain.WorkflowDefinition;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRun;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRunStatus;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRunStep;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRunStepStatus;
import io.odunlamizo.relayflow.workflow.engine.ReplyRouter.ReplyRouting;
import io.odunlamizo.relayflow.workflow.repository.WorkflowRunRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowEngineService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngineService.class);

    private static final int MAX_STEPS = 100;

    private final WorkflowRunRepository runRepository;

    private final ConversationRepository conversationRepository;

    private final ExternalIdentityRepository externalIdentityRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final Map<NodeType, NodeExecutor> executors;

    private final ReplyRouter replyRouter;

    private final WorkflowMessageSender messageSender;

    public WorkflowEngineService(
            WorkflowRunRepository runRepository,
            ConversationRepository conversationRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ApplicationEventPublisher eventPublisher,
            List<NodeExecutor> executorList,
            ReplyRouter replyRouter,
            WorkflowMessageSender messageSender) {
        this.runRepository = runRepository;
        this.conversationRepository = conversationRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.eventPublisher = eventPublisher;
        this.replyRouter = replyRouter;
        this.messageSender = messageSender;
        this.executors =
                executorList.stream()
                        .collect(Collectors.toMap(NodeExecutor::nodeType, Function.identity()));
    }

    /** REQUIRES_NEW isolates each run so one failing workflow never affects another. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeWorkflow(
            WorkflowDefinition definition, Conversation conversation, Message triggeringMessage) {
        executeWorkflowInternal(definition, conversation, triggeringMessage, Map.of());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeWorkflow(
            WorkflowDefinition definition,
            Conversation conversation,
            Message triggeringMessage,
            Map<String, String> additionalVariables) {
        executeWorkflowInternal(definition, conversation, triggeringMessage, additionalVariables);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resumeWorkflow(UUID runId, Message replyMessage) {
        log.info("Resuming workflow run {}", kv("workflowRunId", runId));

        WorkflowRun run = runRepository.findWithDefinitionById(runId).orElse(null);

        if (run == null) {
            log.error("Workflow run not found, cannot resume {}", kv("workflowRunId", runId));

            return;
        }

        if (run.getStatus() != WorkflowRunStatus.WAITING) {
            log.warn(
                    "Workflow run resume skipped, run is not waiting {} {}",
                    kv("workflowRunId", runId),
                    kv("status", run.getStatus()));

            return;
        }

        String waitingNodeId = run.getWaitingAtNodeId();

        if (waitingNodeId == null) {
            log.error("Waiting workflow run has no waiting node {}", kv("workflowRunId", runId));
            run.setStatus(WorkflowRunStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setErrorMessage("Run was WAITING with no waitingAtNodeId");
            runRepository.save(run);

            return;
        }

        List<GraphNode> nodes = parseNodes(run.getWorkflowDefinition().getDraftGraph());
        List<GraphEdge> edges = parseEdges(run.getWorkflowDefinition().getDraftGraph());
        Map<String, GraphNode> nodeMap =
                nodes.stream().collect(Collectors.toMap(GraphNode::id, Function.identity()));
        Map<String, List<GraphEdge>> adjacency = buildAdjacency(edges);

        GraphNode waitingNode = nodeMap.get(waitingNodeId);

        if (waitingNode == null) {
            log.error(
                    "Waiting node not found in workflow graph {} {}",
                    kv("workflowRunId", runId),
                    kv("nodeId", waitingNodeId));
            run.setStatus(WorkflowRunStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setErrorMessage("Waiting node not found: " + waitingNodeId);
            runRepository.save(run);

            return;
        }

        Map<String, Object> snapshot =
                run.getContextSnapshot() != null ? run.getContextSnapshot() : Map.of();
        ExecutionContext context =
                new ExecutionContext(
                        run.getId(),
                        run.getConversation().getId(),
                        run.getWorkspace().getId(),
                        snapshot);

        String replyText = replyMessage.getText() != null ? replyMessage.getText() : "";
        UUID conversationId = run.getConversation().getId();

        try {
            Instant replyStart = Instant.now();
            ReplyRouting routing =
                    replyRouter.route(waitingNode, context, replyText, run.getReplyAttempts());

            if (!routing.output().isEmpty()) {
                WorkflowRunStep step = newStep(waitingNode, run, context);
                finishStep(
                        step, WorkflowRunStepStatus.COMPLETED, routing.output(), replyStart, null);
                run.getSteps().add(step);
            }

            if (routing.isRetry()) {
                run.setReplyAttempts(run.getReplyAttempts() + 1);
                messageSender.send(
                        conversationId,
                        run.getWorkspace().getId(),
                        routing.retryMessage(),
                        List.of());
                runRepository.save(run);

                log.info(
                        "Reply rejected by validation hook, asking again {} {}",
                        kv("workflowRunId", runId),
                        kv("attempt", run.getReplyAttempts()));

                return;
            }

            run.setStatus(WorkflowRunStatus.RUNNING);
            run.setWaitingAtNodeId(null);
            run.setContextSnapshot(null);
            run.setExpiresAt(null);
            run.setReplyAttempts(0);

            GraphNode nextNode = resolveReplyTarget(waitingNode, routing, adjacency, nodeMap);

            if (nextNode != null) {
                walk(nextNode, nodeMap, adjacency, context, run);
            }

            if (run.getStatus() == WorkflowRunStatus.RUNNING) {
                run.setStatus(WorkflowRunStatus.COMPLETED);
                run.setFinishedAt(Instant.now());
                setConversationLock(conversationId, false);
            }

            runRepository.save(run);

            log.info(
                    "Workflow run resumed {} {}",
                    kv("workflowRunId", runId),
                    kv("status", run.getStatus()));
        } catch (Exception exception) {
            run.setStatus(WorkflowRunStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setErrorMessage(exception.getMessage());
            run.setWaitingAtNodeId(null);
            run.setContextSnapshot(null);
            run.setExpiresAt(null);
            runRepository.save(run);

            setConversationLock(conversationId, false);

            log.error("Resumed workflow run failed {}", kv("workflowRunId", runId), exception);
        }
    }

    @Transactional
    public void expireWaitingRun(UUID runId) {
        WorkflowRun run = runRepository.findWithDefinitionById(runId).orElse(null);

        if (run == null || run.getStatus() != WorkflowRunStatus.WAITING) {
            return;
        }

        UUID conversationId = run.getConversation().getId();

        List<GraphNode> nodes = parseNodes(run.getWorkflowDefinition().getDraftGraph());
        List<GraphEdge> edges = parseEdges(run.getWorkflowDefinition().getDraftGraph());
        Map<String, GraphNode> nodeMap =
                nodes.stream().collect(Collectors.toMap(GraphNode::id, Function.identity()));
        Map<String, List<GraphEdge>> adjacency = buildAdjacency(edges);

        GraphNode waitingNode =
                run.getWaitingAtNodeId() != null ? nodeMap.get(run.getWaitingAtNodeId()) : null;
        String errorMessage = noReplyMessage(waitingNode);

        Map<String, Object> snapshot =
                run.getContextSnapshot() != null ? run.getContextSnapshot() : Map.of();
        ExecutionContext context =
                new ExecutionContext(
                        run.getId(), conversationId, run.getWorkspace().getId(), snapshot);

        run.setWaitingAtNodeId(null);
        run.setContextSnapshot(null);
        run.setExpiresAt(null);
        run.setReplyAttempts(0);

        try {
            GraphNode nextNode = null;

            if (waitingNode != null) {
                WorkflowRunStep step = newStep(waitingNode, run, context);
                finishStep(
                        step, WorkflowRunStepStatus.FAILED, Map.of(), Instant.now(), errorMessage);
                run.getSteps().add(step);

                nextNode =
                        findEdgeTarget(
                                waitingNode, ReplyRouter.NO_REPLY_HANDLE, adjacency, nodeMap);

                if (nextNode == null
                        && waitingNode.data().get("validationHook") instanceof String hookKey
                        && !hookKey.isBlank()) {
                    nextNode =
                            findEdgeTarget(
                                    waitingNode, ReplyRouter.INVALID_HANDLE, adjacency, nodeMap);
                }
            }

            if (nextNode == null) {
                failRun(run, errorMessage);
            } else {
                run.setStatus(WorkflowRunStatus.RUNNING);
                walk(nextNode, nodeMap, adjacency, context, run);

                if (run.getStatus() == WorkflowRunStatus.RUNNING) {
                    run.setStatus(WorkflowRunStatus.COMPLETED);
                    run.setFinishedAt(Instant.now());
                    setConversationLock(conversationId, false);
                }

                runRepository.save(run);
            }
        } catch (Exception exception) {
            failRun(run, exception.getMessage());

            log.error("Timed-out workflow run failed {}", kv("workflowRunId", runId), exception);

            return;
        }

        log.info(
                "Workflow run reply timed out {} {}",
                kv("workflowRunId", runId),
                kv("status", run.getStatus()));
    }

    private void failRun(WorkflowRun run, String errorMessage) {
        run.setStatus(WorkflowRunStatus.FAILED);
        run.setFinishedAt(Instant.now());
        run.setErrorMessage(errorMessage);
        runRepository.save(run);

        setConversationLock(run.getConversation().getId(), false);
    }

    private String noReplyMessage(GraphNode waitingNode) {
        if (waitingNode != null
                && waitingNode.data().get("timeoutMinutes") instanceof Number minutes) {
            return "No reply within " + minutes.intValue() + " minutes";
        }

        return "No reply before the timeout";
    }

    @Transactional
    public void failInterruptedRun(UUID runId) {
        WorkflowRun run = runRepository.findWithDefinitionById(runId).orElse(null);

        if (run == null || run.getStatus() != WorkflowRunStatus.RUNNING) {
            return;
        }

        run.setStatus(WorkflowRunStatus.FAILED);
        run.setFinishedAt(Instant.now());
        run.setErrorMessage("Interrupted by server shutdown");
        runRepository.save(run);

        setConversationLock(run.getConversation().getId(), false);

        log.warn("Workflow run interrupted by server shutdown {}", kv("workflowRunId", runId));
    }

    private void executeWorkflowInternal(
            WorkflowDefinition definition,
            Conversation conversation,
            Message triggeringMessage,
            Map<String, String> additionalVariables) {
        if (!definition.isEnabled()) {
            log.debug("Unpublished workflow skipped {}", kv("workflowId", definition.getId()));

            return;
        }

        log.info(
                "Starting workflow run {} {}",
                kv("workflowId", definition.getId()),
                kv("conversationId", conversation.getId()));

        // Re-fetched because the conversation may be detached after an @Async boundary.
        conversation = conversationRepository.findById(conversation.getId()).orElse(conversation);

        List<GraphNode> nodes = parseNodes(definition.getDraftGraph());
        List<GraphEdge> edges = parseEdges(definition.getDraftGraph());

        GraphNode triggerNode =
                nodes.stream()
                        .filter(node -> NodeType.TRIGGER.getValue().equals(node.type()))
                        .findFirst()
                        .orElse(null);

        if (triggerNode == null) {
            log.warn("Workflow skipped, no trigger node {}", kv("workflowId", definition.getId()));

            return;
        }

        Map<String, List<GraphEdge>> adjacency = buildAdjacency(edges);

        Map<String, GraphNode> nodeMap =
                nodes.stream().collect(Collectors.toMap(GraphNode::id, Function.identity()));

        setConversationLock(conversation.getId(), true);

        WorkflowRun run = new WorkflowRun();
        run.setWorkflowDefinition(definition);
        run.setWorkspace(conversation.getWorkspace());
        run.setConversation(conversation);
        run.setStatus(WorkflowRunStatus.RUNNING);
        runRepository.save(run);

        ExecutionContext context =
                buildContext(run, conversation, triggeringMessage, additionalVariables);

        try {
            walk(triggerNode, nodeMap, adjacency, context, run);

            // A WAITING run is completed later by resumeWorkflow.
            if (run.getStatus() == WorkflowRunStatus.RUNNING) {
                run.setStatus(WorkflowRunStatus.COMPLETED);
                run.setFinishedAt(Instant.now());
                setConversationLock(conversation.getId(), false);
            }

            runRepository.save(run);

            log.info(
                    "Workflow run executed {} {}",
                    kv("workflowRunId", run.getId()),
                    kv("status", run.getStatus()));
        } catch (Exception exception) {
            run.setStatus(WorkflowRunStatus.FAILED);
            run.setFinishedAt(Instant.now());
            run.setErrorMessage(exception.getMessage());
            runRepository.save(run);

            setConversationLock(conversation.getId(), false);

            log.error("Workflow run failed {}", kv("workflowRunId", run.getId()), exception);
        }
    }

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

    private GraphNode executeStep(
            GraphNode node,
            Map<String, GraphNode> nodeMap,
            Map<String, List<GraphEdge>> adjacency,
            ExecutionContext context,
            WorkflowRun run) {
        NodeType nodeType = NodeType.fromValue(node.type()).orElse(null);
        NodeExecutor executor = nodeType != null ? executors.get(nodeType) : null;

        if (executor == null) {
            log.warn(
                    "Workflow node skipped, no executor for node type {} {} {}",
                    kv("workflowRunId", run.getId()),
                    kv("nodeId", node.id()),
                    kv("nodeType", node.type()));

            return null;
        }

        WorkflowRunStep step = newStep(node, run, context);
        Instant stepStart = Instant.now();

        try {
            NodeExecutionResult result = executor.execute(node, context);

            finishStep(step, WorkflowRunStepStatus.COMPLETED, result.output(), stepStart, null);
            run.getSteps().add(step);

            log.debug(
                    "Workflow node executed {} {} {}",
                    kv("workflowRunId", run.getId()),
                    kv("nodeId", node.id()),
                    kv("nodeType", node.type()));

            if (result.waiting()) {
                run.setStatus(WorkflowRunStatus.WAITING);
                run.setWaitingAtNodeId(node.id());
                run.setContextSnapshot(context.snapshot());
                run.setExpiresAt(Instant.now().plusSeconds(result.timeoutSeconds()));

                log.debug(
                        "Workflow run waiting for reply {} {}",
                        kv("workflowRunId", run.getId()),
                        kv("nodeId", node.id()));

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
                        "Workflow jump taken {} {} {}",
                        kv("workflowRunId", run.getId()),
                        kv("fromNodeId", node.id()),
                        kv("toNodeId", result.jumpToNodeId()));

                return jumpTarget;
            }

            return resolveNextNode(node, result.nextHandle(), adjacency, nodeMap);
        } catch (Exception exception) {
            finishStep(
                    step,
                    WorkflowRunStepStatus.FAILED,
                    Map.of(),
                    stepStart,
                    exception.getMessage());
            run.getSteps().add(step);

            throw exception;
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
            match =
                    outgoing.stream()
                            .filter(
                                    edge ->
                                            edge.sourceHandle() == null
                                                    || edge.sourceHandle().isBlank())
                            .findFirst()
                            .or(() -> outgoing.stream().findFirst());
        } else {
            match =
                    outgoing.stream()
                            .filter(edge -> nextHandle.equals(edge.sourceHandle()))
                            .findFirst();
        }

        return match.map(edge -> nodeMap.get(edge.target())).orElse(null);
    }

    private GraphNode resolveReplyTarget(
            GraphNode waitingNode,
            ReplyRouting routing,
            Map<String, List<GraphEdge>> adjacency,
            Map<String, GraphNode> nodeMap) {
        String nextHandle = routing.nextHandle();

        if (ReplyRouter.INVALID_HANDLE.equals(nextHandle)) {
            return findEdgeTarget(waitingNode, nextHandle, adjacency, nodeMap);
        }

        if (ReplyRouter.VALID_HANDLE.equals(nextHandle)) {
            GraphNode validTarget = findEdgeTarget(waitingNode, nextHandle, adjacency, nodeMap);

            return validTarget != null
                    ? validTarget
                    : findEdgeTarget(waitingNode, null, adjacency, nodeMap);
        }

        return resolveNextNode(waitingNode, nextHandle, adjacency, nodeMap);
    }

    private GraphNode findEdgeTarget(
            GraphNode node,
            String handle,
            Map<String, List<GraphEdge>> adjacency,
            Map<String, GraphNode> nodeMap) {
        return adjacency.getOrDefault(node.id(), List.of()).stream()
                .filter(
                        edge ->
                                handle == null
                                        ? edge.sourceHandle() == null
                                                || edge.sourceHandle().isBlank()
                                        : handle.equals(edge.sourceHandle()))
                .findFirst()
                .map(edge -> nodeMap.get(edge.target()))
                .orElse(null);
    }

    private ExecutionContext buildContext(
            WorkflowRun run,
            Conversation conversation,
            Message triggeringMessage,
            Map<String, String> additionalVariables) {
        Map<String, Object> vars = new LinkedHashMap<>();

        vars.put("workspace.id", conversation.getWorkspace().getId().toString());
        vars.put("conversation.id", conversation.getId().toString());
        vars.put("conversation.channel", conversation.getChannelAccount().getProvider().name());
        vars.put("contact.id", conversation.getContact().getId().toString());
        vars.put("contact.name", nullSafe(conversation.getContact().getDisplayName()));

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
            vars.put("contact.message", nullSafe(triggeringMessage.getText()));
        }

        for (Map.Entry<String, String> field :
                conversation.getContact().getCustomFields().entrySet()) {
            vars.put("contact.data." + field.getKey(), field.getValue());
        }

        for (Map.Entry<String, String> tag : conversation.getContact().getTags().entrySet()) {
            vars.put("contact.tags." + tag.getKey(), tag.getValue());
        }

        vars.putAll(additionalVariables);

        return new ExecutionContext(
                run.getId(), conversation.getId(), conversation.getWorkspace().getId(), vars);
    }

    private WorkflowRunStep newStep(GraphNode node, WorkflowRun run, ExecutionContext context) {
        WorkflowRunStep step = new WorkflowRunStep();
        step.setRun(run);
        step.setNodeId(node.id());
        step.setNodeType(
                NodeType.fromValue(node.type())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown node type: " + node.type())));
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

    private Map<String, List<GraphEdge>> buildAdjacency(List<GraphEdge> edges) {
        Map<String, List<GraphEdge>> adjacency = new HashMap<>();

        for (GraphEdge edge : edges) {
            adjacency.computeIfAbsent(edge.source(), key -> new ArrayList<>()).add(edge);
        }

        return adjacency;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private void setConversationLock(UUID conversationId, boolean locked) {
        conversationRepository
                .findById(conversationId)
                .ifPresent(
                        conversation -> {
                            conversation.setLockedByWorkflow(locked);
                            conversationRepository.save(conversation);

                            eventPublisher.publishEvent(
                                    new SseBroadcastEvent(
                                            conversation.getWorkspace().getId(),
                                            SseEventType.CONVERSATION_UPDATED,
                                            Map.of(
                                                    "workspaceId",
                                                            conversation
                                                                    .getWorkspace()
                                                                    .getId()
                                                                    .toString(),
                                                    "conversationId", conversationId.toString())));
                        });
    }
}
