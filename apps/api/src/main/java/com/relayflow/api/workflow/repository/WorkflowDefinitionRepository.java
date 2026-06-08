package com.relayflow.api.workflow.repository;

import com.relayflow.api.workflow.domain.WorkflowDefinition;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    @Query(
            "select w from WorkflowDefinition w where w.workspace.id = :workspaceId order by w.createdAt asc")
    List<WorkflowDefinition> findByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("select w from WorkflowDefinition w where w.id = :id and w.workspace.id = :workspaceId")
    Optional<WorkflowDefinition> findInWorkspace(
            @Param("id") UUID id, @Param("workspaceId") UUID workspaceId);

    @Query(
            "select w from WorkflowDefinition w where w.workspace.id = :workspaceId and w.enabled = true order by w.createdAt asc")
    List<WorkflowDefinition> findEnabledByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query("UPDATE WorkflowDefinition w SET w.deletedAt = :now WHERE w.workspace.id = :workspaceId")
    void softDeleteByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);

    /** Counts non-deleted workflow definitions in a workspace for plan limit enforcement. */
    @Query("select count(w) from WorkflowDefinition w where w.workspace.id = :workspaceId")
    long countByWorkspace(@Param("workspaceId") UUID workspaceId);

    /** Counts currently-enabled workflows — used when re-enabling to enforce the plan limit. */
    @Query(
            "select count(w) from WorkflowDefinition w where w.workspace.id = :workspaceId and w.enabled = true")
    long countEnabled(@Param("workspaceId") UUID workspaceId);

    /**
     * Returns all non-deleted workflows ordered newest-first. Used by the downgrade job to pick the
     * excess workflows to disable (beyond the FREE limit).
     */
    @Query(
            "select w from WorkflowDefinition w where w.workspace.id = :workspaceId order by w.createdAt desc")
    List<WorkflowDefinition> findNewestFirst(@Param("workspaceId") UUID workspaceId);
}
