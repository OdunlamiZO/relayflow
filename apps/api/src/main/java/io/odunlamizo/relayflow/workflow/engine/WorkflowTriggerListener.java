package io.odunlamizo.relayflow.workflow.engine;

// Docs: docs/codebase-components.md#workflowtriggerlistener

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.agent.repository.AiAgentInvocationLogRepository;
import io.odunlamizo.relayflow.workflow.NodeType;
import io.odunlamizo.relayflow.workflow.domain.WorkflowDefinition;
import io.odunlamizo.relayflow.workflow.repository.WorkflowDefinitionRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WorkflowTriggerListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTriggerListener.class);

    static final String TRIGGER_EVENT_CONVERSATION_OPENED = "conversation_opened";

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    private final WorkflowEngineService workflowEngineService;

    private final AiAgentInvocationLogRepository aiAgentInvocationLogRepository;

    public WorkflowTriggerListener(
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowEngineService workflowEngineService,
            AiAgentInvocationLogRepository aiAgentInvocationLogRepository) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowEngineService = workflowEngineService;
        this.aiAgentInvocationLogRepository = aiAgentInvocationLogRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConversationOpened(ConversationOpenedEvent event) {
        var conversation = event.conversation();
        var workspaceId = conversation.getWorkspace().getId();

        if (aiAgentInvocationLogRepository.existsActiveForConversation(conversation.getId())) {
            log.debug(
                    "Workflow trigger skipped, AI agent is active {}",
                    kv("conversationId", conversation.getId()));

            return;
        }

        List<WorkflowDefinition> workflows =
                workflowDefinitionRepository.findEnabledByWorkspace(workspaceId).stream()
                        .filter(
                                definition ->
                                        hasTriggerEvent(
                                                definition, TRIGGER_EVENT_CONVERSATION_OPENED))
                        .toList();

        if (workflows.isEmpty()) {
            return;
        }

        log.info(
                "Triggering workflows {} {} {} {}",
                kv("workspaceId", workspaceId),
                kv("conversationId", conversation.getId()),
                kv("trigger", "conversation_opened"),
                kv("count", workflows.size()));

        for (WorkflowDefinition workflow : workflows) {
            try {
                workflowEngineService.executeWorkflow(
                        workflow, conversation, event.triggeringMessage());
            } catch (Exception exception) {
                log.error(
                        "Workflow execution failed {} {}",
                        kv("workflowId", workflow.getId()),
                        kv("conversationId", conversation.getId()),
                        exception);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean hasTriggerEvent(WorkflowDefinition definition, String event) {
        var graph = definition.getDraftGraph();

        if (graph == null) {
            return false;
        }

        List<Map<String, Object>> nodes =
                (List<Map<String, Object>>) graph.getOrDefault("nodes", List.of());

        return nodes.stream()
                .filter(node -> NodeType.TRIGGER.getValue().equals(node.get("type")))
                .anyMatch(
                        node -> {
                            Map<String, Object> data =
                                    (Map<String, Object>) node.getOrDefault("data", Map.of());

                            return event.equals(data.get("event"));
                        });
    }
}
