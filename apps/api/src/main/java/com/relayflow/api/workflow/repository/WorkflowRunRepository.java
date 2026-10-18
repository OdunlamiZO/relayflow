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

    @Modifying
    @Query(
            """
            update WorkflowRun r
            set r.status = :failed, r.finishedAt = :now, r.errorMessage = :reason
            where r.conversation.id in :conversationIds and r.status in :activeStatuses
            """)
    void failActiveForConversations(
            @Param("conversationIds") Collection<UUID> conversationIds,
            @Param("activeStatuses") Collection<WorkflowRunStatus> activeStatuses,
            @Param("failed") WorkflowRunStatus failed,
            @Param("reason") String reason,
            @Param("now") Instant now);

    @Query(
            """
            select r from WorkflowRun r
            join fetch r.workflowDefinition
            join fetch r.conversation c
            join fetch c.workspace
            where r.id = :id
            """)
    Optional<WorkflowRun> findWithDefinitionById(@Param("id") UUID id);

    @Query(
            "select r from WorkflowRun r where r.workflowDefinition.id = :workflowId and r.workspace.id = :workspaceId order by r.startedAt desc")
    List<WorkflowRun> findByWorkflowAndWorkspace(
            @Param("workflowId") UUID workflowId,
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable);

    @Query(
            "select distinct r from WorkflowRun r join fetch r.steps where r.id = :id and r.workflowDefinition.id = :workflowId and r.workspace.id = :workspaceId")
    Optional<WorkflowRun> findRunWithSteps(
            @Param("id") UUID id,
            @Param("workflowId") UUID workflowId,
            @Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query("delete from WorkflowRun r where r.startedAt < :cutoff")
    int deleteByStartedAtBefore(@Param("cutoff") Instant cutoff);

    @Query("select r.id from WorkflowRun r where r.status = :status and r.expiresAt < :now")
    List<UUID> findExpiredWaitingRunIds(
            @Param("status") WorkflowRunStatus status, @Param("now") Instant now);

    @Query(
            """
            select r from WorkflowRun r
            join fetch r.conversation c
            join fetch c.workspace
            where r.status = :status
            """)
    List<WorkflowRun> findAllWithConversationByStatus(@Param("status") WorkflowRunStatus status);
}
