package com.relayflow.api.workflow.repository;

import com.relayflow.api.workflow.domain.WorkflowRunStep;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRunStepRepository extends JpaRepository<WorkflowRunStep, UUID> {}
