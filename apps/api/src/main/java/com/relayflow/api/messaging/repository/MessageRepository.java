package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.Message;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Returns the most recent {@code pageable.pageSize} messages for the given conversation,
     * ordered newest-first. The caller reverses the list before returning to the client.
     */
    @Query(
            """
            select m from Message m
            where m.conversation.id = :conversationId and m.workspace.id = :workspaceId
            order by m.createdAt desc
            """)
    List<Message> findRecentByConversation(
            @Param("conversationId") UUID conversationId,
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable);

    /**
     * Returns messages strictly before {@code before}, ordered newest-first. Used for cursor-based
     * pagination to load older messages.
     */
    @Query(
            """
            select m from Message m
            where m.conversation.id = :conversationId
              and m.workspace.id = :workspaceId
              and m.createdAt < :before
            order by m.createdAt desc
            """)
    List<Message> findBeforeCursor(
            @Param("conversationId") UUID conversationId,
            @Param("workspaceId") UUID workspaceId,
            @Param("before") Instant before,
            Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.deletedAt = :now WHERE m.workspace.id = :workspaceId")
    void softDeleteByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);
}
