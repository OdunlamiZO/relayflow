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

    List<AiAgentConfiguration> findAllByWorkspaceId(UUID workspaceId);

    Optional<AiAgentConfiguration> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<AiAgentConfiguration> findByWorkspaceIdAndDefaultConfigTrue(UUID workspaceId);

    @Modifying
    @Query(
            "UPDATE AiAgentConfiguration c SET c.defaultConfig = false"
                    + " WHERE c.workspace.id = :workspaceId AND c.defaultConfig = true")
    void clearDefault(@Param("workspaceId") UUID workspaceId);

    void deleteByWorkspaceId(UUID workspaceId);
}
