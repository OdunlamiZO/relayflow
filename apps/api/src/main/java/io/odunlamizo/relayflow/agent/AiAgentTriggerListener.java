package io.odunlamizo.relayflow.agent;

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.agent.repository.AiAgentInvocationLogRepository;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRunStatus;
import io.odunlamizo.relayflow.workflow.engine.ConversationMessageReceivedEvent;
import io.odunlamizo.relayflow.workflow.engine.ConversationOpenedEvent;
import io.odunlamizo.relayflow.workflow.repository.WorkflowRunRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Guards are re-checked inside AiAgentInvocationService's transaction to close the race window. */
@Component
public class AiAgentTriggerListener {

    private static final Logger log = LoggerFactory.getLogger(AiAgentTriggerListener.class);

    private final AiAgentConfigurationService aiAgentConfigurationService;

    private final AiAgentInvocationLogRepository invocationLogRepository;

    private final WorkflowRunRepository workflowRunRepository;

    private final AiAgentInvocationService invocationService;

    public AiAgentTriggerListener(
            AiAgentConfigurationService aiAgentConfigurationService,
            AiAgentInvocationLogRepository invocationLogRepository,
            WorkflowRunRepository workflowRunRepository,
            AiAgentInvocationService invocationService) {
        this.aiAgentConfigurationService = aiAgentConfigurationService;
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
                "AI agent triggered {} {}",
                kv("trigger", "conversation_opened"),
                kv("conversationId", conversation.getId()));

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
                "AI agent triggered {} {}",
                kv("trigger", "message_received"),
                kv("conversationId", conversation.getId()));

        invocationService.invoke(conversation, event.message());
    }

    private boolean shouldInvoke(Conversation conversation) {
        if (conversation.getAssigneeId() != null) {
            return false;
        }

        UUID workspaceId = conversation.getWorkspace().getId();
        UUID channelAccountId = conversation.getChannelAccount().getId();
        UUID conversationId = conversation.getId();

        if (aiAgentConfigurationService
                .resolveEnabledConfiguration(workspaceId, channelAccountId)
                .isEmpty()) {
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
