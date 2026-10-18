package com.relayflow.api.workflow.repository;

import com.relayflow.api.workflow.domain.WorkflowRunStep;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowRunStepRepository extends JpaRepository<WorkflowRunStep, UUID> {

    @Modifying
    @Query("delete from WorkflowRunStep s where s.run.startedAt < :cutoff")
    int deleteByRunStartedAtBefore(@Param("cutoff") Instant cutoff);
}
