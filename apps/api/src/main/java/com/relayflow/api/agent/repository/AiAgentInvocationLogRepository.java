package com.relayflow.api.agent.repository;

import com.relayflow.api.agent.domain.AiAgentInvocationLog;
import com.relayflow.api.agent.domain.AiAgentInvocationStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiAgentInvocationLogRepository extends JpaRepository<AiAgentInvocationLog, UUID> {

    Optional<AiAgentInvocationLog> findByConversationIdAndStatus(
            UUID conversationId, AiAgentInvocationStatus status);

    @Query(
            """
            select count(l) > 0 from AiAgentInvocationLog l
            where l.conversation.id = :conversationId
              and l.status in ('RUNNING', 'CLARIFYING')
            """)
    boolean existsActiveForConversation(@Param("conversationId") UUID conversationId);

    @Modifying
    @Query("delete from AiAgentInvocationLog l where l.startedAt < :cutoff")
    int deleteByStartedAtBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from AiAgentInvocationLog l where l.workspace.id = :workspaceId")
    void deleteByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
