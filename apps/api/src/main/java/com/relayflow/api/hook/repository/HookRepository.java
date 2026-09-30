package com.relayflow.api.hook.repository;

import com.relayflow.api.hook.domain.Hook;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HookRepository extends JpaRepository<Hook, UUID> {

    @Query("select h from Hook h where h.workspaceId = :workspaceId order by h.name asc")
    List<Hook> findByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("select h from Hook h where h.id = :id and h.workspaceId = :workspaceId")
    Optional<Hook> findInWorkspace(@Param("id") UUID id, @Param("workspaceId") UUID workspaceId);

    @Query(
            value =
                    "select name from workflow_definitions where workspace_id = :workspaceId and"
                            + " draft_graph::text like concat('%\"', :hookKey, '\"%') order by name",
            nativeQuery = true)
    List<String> findWorkflowNamesReferencing(
            @Param("workspaceId") UUID workspaceId, @Param("hookKey") String hookKey);

    @Query(
            value =
                    "select name from ai_agent_configs where workspace_id = :workspaceId and"
                            + " extraction_fields::text like concat('%\"', :hookKey, '\"%')"
                            + " order by name",
            nativeQuery = true)
    List<String> findAiAgentNamesReferencing(
            @Param("workspaceId") UUID workspaceId, @Param("hookKey") String hookKey);
}
