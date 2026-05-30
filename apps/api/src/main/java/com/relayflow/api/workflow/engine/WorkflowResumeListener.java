package com.relayflow.api.workflow.engine;

import com.relayflow.api.workflow.domain.WorkflowRun;
import com.relayflow.api.workflow.domain.WorkflowRunStatus;
import com.relayflow.api.workflow.repository.WorkflowRunRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Resumes workflow runs that are paused at a "Wait for Reply" node when the contact sends a reply.
 *
 * <p>Listens for {@link ConversationMessageReceivedEvent} — published (after commit) whenever a
 * message arrives in an <em>already-open</em> conversation. Each waiting run in that conversation
 * is resumed independently; a failure in one does not prevent the others from continuing.
 */
@Component
public class WorkflowResumeListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowResumeListener.class);

    private final WorkflowRunRepository runRepository;

    private final WorkflowEngineService workflowEngineService;

    public WorkflowResumeListener(
            WorkflowRunRepository runRepository, WorkflowEngineService workflowEngineService) {
        this.runRepository = runRepository;
        this.workflowEngineService = workflowEngineService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageReceived(ConversationMessageReceivedEvent event) {
        var conversationId = event.conversation().getId();

        List<WorkflowRun> waitingRuns =
                runRepository.findForConversationWithStatus(
                        conversationId, WorkflowRunStatus.WAITING);

        if (waitingRuns.isEmpty()) {
            return;
        }

        log.info(
                "Resuming {} waiting workflow run(s) for conversation={}",
                waitingRuns.size(),
                conversationId);

        for (WorkflowRun run : waitingRuns) {
            try {
                workflowEngineService.resumeWorkflow(run.getId(), event.message());
            } catch (Exception e) {
                log.error("Failed to resume workflow run={}: {}", run.getId(), e.getMessage(), e);
            }
        }
    }
}
