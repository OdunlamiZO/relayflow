package io.odunlamizo.relayflow.workflow;

// Docs: docs/codebase-components.md#workflowstartuprecovery

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.workflow.domain.WorkflowRun;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRunStatus;
import io.odunlamizo.relayflow.workflow.engine.WorkflowEngineService;
import io.odunlamizo.relayflow.workflow.repository.WorkflowRunRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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
                    "Interrupted workflow runs recovered {}",
                    kv("count", interruptedRunIds.size()));
        }
    }
}
