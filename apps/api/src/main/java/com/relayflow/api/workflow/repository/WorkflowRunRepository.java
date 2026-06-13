package com.relayflow.api.workflow.repository;

import com.relayflow.api.workflow.domain.WorkflowRun;
import com.relayflow.api.workflow.domain.WorkflowRunStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /** Returns runs for a workflow definition, newest first, for the run logs UI. */
    @Query(
            "select r from WorkflowRun r where r.workflowDefinition.id = :workflowId and r.workspace.id = :workspaceId order by r.startedAt desc")
    List<WorkflowRun> findByWorkflowAndWorkspace(
            @Param("workflowId") UUID workflowId,
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable);

    /** Loads a single run with its steps eagerly — used for the run detail view. */
    @Query(
            "select distinct r from WorkflowRun r join fetch r.steps where r.id = :id and r.workflowDefinition.id = :workflowId and r.workspace.id = :workspaceId")
    Optional<WorkflowRun> findRunWithSteps(
            @Param("id") UUID id,
            @Param("workflowId") UUID workflowId,
            @Param("workspaceId") UUID workspaceId);

    /** Deletes runs started before {@code cutoff} — run log retention cleanup. */
    @Modifying
    @Query("delete from WorkflowRun r where r.startedAt < :cutoff")
    int deleteByStartedAtBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from WorkflowRun r where r.conversation.id in :conversationIds")
    void deleteByConversations(@Param("conversationIds") Collection<UUID> conversationIds);

    /** Returns the ids of WAITING runs whose "Wait for Reply" timeout has elapsed. */
    @Query("select r.id from WorkflowRun r where r.status = :status and r.expiresAt < :now")
    List<UUID> findExpiredWaitingRunIds(
            @Param("status") WorkflowRunStatus status, @Param("now") Instant now);
}
