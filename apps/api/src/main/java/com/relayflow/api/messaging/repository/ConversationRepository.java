package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.Contact;
import com.relayflow.api.messaging.domain.Conversation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query(
            """
            select c from Conversation c
            where c.workspace.id = :workspaceId
            order by c.lastMessageAt desc nulls last, c.createdAt desc
            """)
    List<Conversation> findByWorkspace(@Param("workspaceId") UUID workspaceId);

    /** Offset-based paginated overload of {@link #findByWorkspace(UUID)}. */
    @Query(
            """
            select c from Conversation c
            where c.workspace.id = :workspaceId
            order by c.lastMessageAt desc nulls last, c.createdAt desc
            """)
    List<Conversation> findByWorkspace(@Param("workspaceId") UUID workspaceId, Pageable pageable);

    @Query("select c from Conversation c where c.id = :id and c.workspace.id = :workspaceId")
    Optional<Conversation> findInWorkspace(
            @Param("id") UUID id, @Param("workspaceId") UUID workspaceId);

    @Query(
            """
            select c from Conversation c
            where c.workspace.id = :workspaceId
              and c.contact.id = :contactId
            order by c.lastMessageAt desc nulls last, c.createdAt desc
            """)
    List<Conversation> findByWorkspaceAndContact(
            @Param("workspaceId") UUID workspaceId,
            @Param("contactId") UUID contactId,
            Pageable pageable);

    @Modifying
    @Query("UPDATE Conversation c SET c.deletedAt = :now WHERE c.workspace.id = :workspaceId")
    void softDeleteByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Conversation c SET c.contact = :target WHERE c.contact.id = :sourceId")
    void reassignContact(@Param("target") Contact target, @Param("sourceId") UUID sourceId);

    /**
     * Returns the most recent conversation for a contact on a given channel, regardless of status.
     * Used to reopen a closed conversation when the contact sends a new message.
     */
    @Query(
            """
            select c from Conversation c
            where c.workspace.id = :workspaceId
              and c.channelAccount.id = :channelAccountId
              and c.contact.id = :contactId
            order by c.lastMessageAt desc nulls last, c.createdAt desc
            """)
    List<Conversation> findLatestConversationForContact(
            @Param("workspaceId") UUID workspaceId,
            @Param("channelAccountId") UUID channelAccountId,
            @Param("contactId") UUID contactId,
            Pageable pageable);
}
