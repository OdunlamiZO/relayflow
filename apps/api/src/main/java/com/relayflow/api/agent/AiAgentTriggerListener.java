package com.relayflow.api.agent;

import com.relayflow.api.agent.repository.AiAgentConfigurationRepository;
import com.relayflow.api.agent.repository.AiAgentInvocationLogRepository;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.workflow.domain.WorkflowRunStatus;
import com.relayflow.api.workflow.engine.ConversationMessageReceivedEvent;
import com.relayflow.api.workflow.engine.ConversationOpenedEvent;
import com.relayflow.api.workflow.repository.WorkflowRunRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Triggers AI agent processing for new conversations and inbound messages.
 *
 * <p>Runs {@code AFTER_COMMIT} (all DB writes are durable) and {@code @Async} (never blocks the
 * webhook thread). Guards are checked before delegating to {@link AiAgentInvocationService}, which
 * also re-checks them inside its own transaction to close the race window.
 */
@Component
public class AiAgentTriggerListener {

    private static final Logger log = LoggerFactory.getLogger(AiAgentTriggerListener.class);

    private final AiAgentConfigurationRepository configurationRepository;

    private final AiAgentInvocationLogRepository invocationLogRepository;

    private final WorkflowRunRepository workflowRunRepository;

    private final AiAgentInvocationService invocationService;

    public AiAgentTriggerListener(
            AiAgentConfigurationRepository configurationRepository,
            AiAgentInvocationLogRepository invocationLogRepository,
            WorkflowRunRepository workflowRunRepository,
            AiAgentInvocationService invocationService) {
        this.configurationRepository = configurationRepository;
        this.invocationLogRepository = invocationLogRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.invocationService = invocationService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConversationOpened(ConversationOpenedEvent event) {
        var conversation = event.conversation();

        if (!shouldInvoke(conversation)) {
            return;
        }

        log.debug(
                "AI agent triggered by conversation_opened for conversation={}",
                conversation.getId());

        invocationService.invoke(conversation, event.triggeringMessage());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageReceived(ConversationMessageReceivedEvent event) {
        var conversation = event.conversation();

        if (!shouldInvoke(conversation)) {
            return;
        }

        log.debug(
                "AI agent triggered by message_received for conversation={}", conversation.getId());

        invocationService.invoke(conversation, event.message());
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private boolean shouldInvoke(Conversation conversation) {
        if (conversation.getAssigneeId() != null) {
            return false;
        }

        UUID workspaceId = conversation.getWorkspace().getId();
        UUID conversationId = conversation.getId();

        if (configurationRepository.findByWorkspaceIdAndEnabledTrue(workspaceId).isEmpty()) {
            return false;
        }

        boolean workflowActive =
                !workflowRunRepository
                                .findForConversationWithStatus(
                                        conversationId, WorkflowRunStatus.RUNNING)
                                .isEmpty()
                        || !workflowRunRepository
                                .findForConversationWithStatus(
                                        conversationId, WorkflowRunStatus.WAITING)
                                .isEmpty();

        return !workflowActive;
    }
}
