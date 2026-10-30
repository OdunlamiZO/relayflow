package io.odunlamizo.relayflow.channel.repository;

// Docs: docs/codebase-components.md#channel-repositories

import io.odunlamizo.relayflow.agent.domain.AiAgentConfiguration;
import io.odunlamizo.relayflow.channel.domain.ChannelAccount;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChannelAccountRepository extends JpaRepository<ChannelAccount, UUID> {

    @Modifying
    @Query("UPDATE ChannelAccount ca SET ca.deletedAt = :now WHERE ca.workspace.id = :workspaceId")
    void softDeleteByWorkspace(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);

    @Query("select ca from ChannelAccount ca where ca.workspace.id = :workspaceId")
    List<ChannelAccount> findByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("select ca from ChannelAccount ca where ca.id = :id and ca.workspace.id = :workspaceId")
    Optional<ChannelAccount> findInWorkspace(
            @Param("id") UUID id, @Param("workspaceId") UUID workspaceId);

    /** Safe to call with a detached ID. */
    @Query("select ca.aiAgentConfiguration from ChannelAccount ca where ca.id = :channelAccountId")
    Optional<AiAgentConfiguration> findAiAgentConfiguration(
            @Param("channelAccountId") UUID channelAccountId);
}
