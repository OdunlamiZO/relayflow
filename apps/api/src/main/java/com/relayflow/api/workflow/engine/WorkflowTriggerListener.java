package com.relayflow.api.workflow.engine;

import com.relayflow.api.agent.repository.AiAgentInvocationLogRepository;
import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.domain.WorkflowDefinition;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fires enabled workflows whenever a new conversation is opened by an inbound message.
 *
 * <p>Uses {@code AFTER_COMMIT} so the conversation and triggering message are fully persisted
 * before the workflow engine queries them, and {@code @Async} so workflow execution never blocks
 * the Telegram webhook thread.
 *
 * <p>Only workflows whose trigger node declares {@code event = "conversation_opened"} are executed.
 * Other trigger types (e.g. "message_received") will be handled by their own dedicated listeners as
 * they are added, each filtering by their own event value.
 */
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

    /**
     * Handles a conversation-opened event by finding and executing all enabled workflows whose
     * trigger node is configured for the {@code conversation_opened} event.
     *
     * <p>Each workflow is executed independently — a failure in one does not prevent the others
     * from running.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConversationOpened(ConversationOpenedEvent event) {
        var conversation = event.conversation();
        var workspaceId = conversation.getWorkspace().getId();

        if (aiAgentInvocationLogRepository.existsActiveForConversation(conversation.getId())) {
            log.debug(
                    "Skipping workflow trigger for conversation={} — AI agent is already active",
                    conversation.getId());

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
                "Triggering {} conversation_opened workflow(s) for conversation={} workspace={}",
                workflows.size(),
                conversation.getId(),
                workspaceId);

        for (WorkflowDefinition workflow : workflows) {
            try {
                workflowEngineService.executeWorkflow(
                        workflow, conversation, event.triggeringMessage());
            } catch (Exception e) {
                log.error(
                        "Uncaught exception running workflow={} for conversation={}: {}",
                        workflow.getId(),
                        conversation.getId(),
                        e.getMessage(),
                        e);
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the workflow's trigger node declares the given event value. Reads
     * directly from {@code draftGraph} so no additional column is needed.
     */
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
