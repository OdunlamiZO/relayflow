package com.relayflow.api.workflow.repository;

import com.relayflow.api.workflow.domain.WorkflowRun;
import com.relayflow.api.workflow.domain.WorkflowRunStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, UUID> {

    /**
     * Returns all runs whose status matches {@code status} in the given conversation. The workflow
     * definition is eagerly fetched so the engine can access the graph without an extra query.
     */
    @Query(
            """
            select r from WorkflowRun r
            join fetch r.workflowDefinition
            join fetch r.conversation c
            join fetch c.workspace
            where r.conversation.id = :conversationId
              and r.status = :status
            """)
    List<WorkflowRun> findForConversationWithStatus(
            @Param("conversationId") UUID conversationId,
            @Param("status") WorkflowRunStatus status);

    /**
     * Loads a single run with its workflow definition and conversation eagerly — used when resuming
     * a run inside a fresh transaction.
     */
    @Query(
            """
            select r from WorkflowRun r
            join fetch r.workflowDefinition
            join fetch r.conversation c
            join fetch c.workspace
            where r.id = :id
            """)
    Optional<WorkflowRun> findWithDefinitionById(@Param("id") UUID id);
}
