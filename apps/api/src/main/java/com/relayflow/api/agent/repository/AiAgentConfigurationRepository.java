package com.relayflow.api.agent.repository;

import com.relayflow.api.agent.domain.AiAgentConfiguration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAgentConfigurationRepository extends JpaRepository<AiAgentConfiguration, UUID> {

    Optional<AiAgentConfiguration> findByWorkspaceId(UUID workspaceId);

    Optional<AiAgentConfiguration> findByWorkspaceIdAndEnabledTrue(UUID workspaceId);

    void deleteByWorkspaceId(UUID workspaceId);
}
