package com.relayflow.api.workflow;

import com.relayflow.api.workflow.domain.WorkflowRunStatus;
import com.relayflow.api.workflow.engine.WorkflowEngineService;
import com.relayflow.api.workflow.repository.WorkflowRunRepository;
import com.relayflow.api.workflow.repository.WorkflowRunStepRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkflowRunCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRunCleanupScheduler.class);

    private final WorkflowRunRepository workflowRunRepository;

    private final WorkflowRunStepRepository workflowRunStepRepository;

    private final WorkflowEngineService workflowEngineService;

    private final long runRetentionDays;

    public WorkflowRunCleanupScheduler(
            WorkflowRunRepository workflowRunRepository,
            WorkflowRunStepRepository workflowRunStepRepository,
            WorkflowEngineService workflowEngineService,
            @Value("${relayflow.workflow.run-retention-days}") long runRetentionDays) {
        this.workflowRunRepository = workflowRunRepository;
        this.workflowRunStepRepository = workflowRunStepRepository;
        this.workflowEngineService = workflowEngineService;
        this.runRetentionDays = runRetentionDays;
    }

    /**
     * Runs every hour, removes workflow runs (and their steps) started more than {@code
     * relayflow.workflow.run-retention-days} ago.
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    @Transactional
    public void cleanupExpiredRuns() {
        Instant cutoff = Instant.now().minus(runRetentionDays, ChronoUnit.DAYS);

        int deletedSteps = workflowRunStepRepository.deleteByRunStartedAtBefore(cutoff);
        int deletedRuns = workflowRunRepository.deleteByStartedAtBefore(cutoff);

        if (deletedRuns > 0) {
            log.info(
                    "Cleaned up {} expired workflow run(s) and {} step(s)",
                    deletedRuns,
                    deletedSteps);
        }
    }

    /**
     * Runs every minute, fails any "Wait for Reply" runs that have been WAITING past their
     * configured timeout.
     */
    @Scheduled(fixedDelay = 60 * 1000)
    public void failExpiredWaitingRuns() {
        List<UUID> expiredRunIds =
                workflowRunRepository.findExpiredWaitingRunIds(
                        WorkflowRunStatus.WAITING, Instant.now());

        for (UUID runId : expiredRunIds) {
            workflowEngineService.expireWaitingRun(runId);
        }

        if (!expiredRunIds.isEmpty()) {
            log.info(
                    "Failed {} workflow run(s) that timed out waiting for a reply",
                    expiredRunIds.size());
        }
    }
}
