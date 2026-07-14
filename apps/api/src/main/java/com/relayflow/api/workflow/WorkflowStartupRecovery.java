package com.relayflow.api.workflow;

import com.relayflow.api.workflow.domain.WorkflowRun;
import com.relayflow.api.workflow.domain.WorkflowRunStatus;
import com.relayflow.api.workflow.engine.WorkflowEngineService;
import com.relayflow.api.workflow.repository.WorkflowRunRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Recovers workflow runs that were left in {@code RUNNING} state by a previous server crash or
 * forced kill. Marks them {@code FAILED} and releases their conversation locks so contacts are not
 * permanently locked out of sending messages.
 */
@Component
public class WorkflowStartupRecovery {

    private static final Logger log = LoggerFactory.getLogger(WorkflowStartupRecovery.class);

    private final WorkflowRunRepository workflowRunRepository;

    private final WorkflowEngineService workflowEngineService;

    public WorkflowStartupRecovery(
            WorkflowRunRepository workflowRunRepository,
            WorkflowEngineService workflowEngineService) {
        this.workflowRunRepository = workflowRunRepository;
        this.workflowEngineService = workflowEngineService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedRuns() {
        List<UUID> interruptedRunIds =
                workflowRunRepository
                        .findAllWithConversationByStatus(WorkflowRunStatus.RUNNING)
                        .stream()
                        .map(WorkflowRun::getId)
                        .toList();

        for (UUID runId : interruptedRunIds) {
            workflowEngineService.failInterruptedRun(runId);
        }

        if (!interruptedRunIds.isEmpty()) {
            log.warn(
                    "Recovered {} workflow run(s) interrupted by previous crash",
                    interruptedRunIds.size());
        }
    }
}
