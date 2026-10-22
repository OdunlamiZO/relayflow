package com.relayflow.api.workflow.engine;

import static net.logstash.logback.argument.StructuredArguments.kv;

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
                "Resuming waiting workflow runs {} {}",
                kv("conversationId", conversationId),
                kv("count", waitingRuns.size()));

        for (WorkflowRun run : waitingRuns) {
            try {
                workflowEngineService.resumeWorkflow(run.getId(), event.message());
            } catch (Exception exception) {
                log.error(
                        "Workflow run resume failed {}",
                        kv("workflowRunId", run.getId()),
                        exception);
            }
        }
    }
}
