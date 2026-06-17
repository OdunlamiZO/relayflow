package com.relayflow.api.agent;

import com.relayflow.api.agent.repository.AiAgentInvocationLogRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AiAgentInvocationCleanupScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(AiAgentInvocationCleanupScheduler.class);

    private final AiAgentInvocationLogRepository invocationLogRepository;

    private final long retentionDays;

    public AiAgentInvocationCleanupScheduler(
            AiAgentInvocationLogRepository invocationLogRepository,
            @Value("${relayflow.agent.invocation-log-retention-days}") long retentionDays) {
        this.invocationLogRepository = invocationLogRepository;
        this.retentionDays = retentionDays;
    }

    /** Runs every hour, removes invocation logs older than {@code retentionDays}. */
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    @Transactional
    public void cleanupExpiredLogs() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = invocationLogRepository.deleteByStartedAtBefore(cutoff);

        if (deleted > 0) {
            log.info("Cleaned up {} expired AI agent invocation log(s)", deleted);
        }
    }
}
