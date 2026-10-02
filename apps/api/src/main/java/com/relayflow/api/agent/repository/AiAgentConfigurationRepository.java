package com.relayflow.api.agent.repository;

import com.relayflow.api.agent.domain.AiAgentConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiAgentConfigurationRepository extends JpaRepository<AiAgentConfiguration, UUID> {

    @Query(
            "select c from AiAgentConfiguration c where c.workspace.id = :workspaceId"
                    + " order by c.createdAt asc")
    List<AiAgentConfiguration> findByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query(
            "select c from AiAgentConfiguration c where c.id = :id and c.workspace.id = :workspaceId")
    Optional<AiAgentConfiguration> findInWorkspace(
            @Param("id") UUID id, @Param("workspaceId") UUID workspaceId);

    @Query(
            "select c from AiAgentConfiguration c where c.workspace.id = :workspaceId"
                    + " and c.defaultConfig = true")
    Optional<AiAgentConfiguration> findDefault(@Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query(
            "UPDATE AiAgentConfiguration c SET c.defaultConfig = false"
                    + " WHERE c.workspace.id = :workspaceId AND c.defaultConfig = true")
    void clearDefault(@Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query("delete from AiAgentConfiguration c where c.workspace.id = :workspaceId")
    void deleteByWorkspace(@Param("workspaceId") UUID workspaceId);
}
