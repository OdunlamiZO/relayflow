package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.ChannelProvider;
import com.relayflow.api.messaging.domain.ExternalIdentity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, UUID> {

    @Modifying
    @Query("UPDATE ExternalIdentity e SET e.deletedAt = :now WHERE e.workspace.id = :workspaceId")
    void softDeleteByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);

    @Query(
            """
            select e from ExternalIdentity e
            where e.workspace.id = :workspaceId
              and e.provider = :provider
              and e.externalUserId = :externalUserId
            """)
    Optional<ExternalIdentity> findForExternalUser(
            @Param("workspaceId") UUID workspaceId,
            @Param("provider") ChannelProvider provider,
            @Param("externalUserId") String externalUserId);

    @Query(
            """
            select e from ExternalIdentity e
            where e.workspace.id = :workspaceId
              and e.provider = :provider
              and e.contact.id = :contactId
            """)
    Optional<ExternalIdentity> findForContact(
            @Param("workspaceId") UUID workspaceId,
            @Param("provider") ChannelProvider provider,
            @Param("contactId") UUID contactId);

    /**
     * Returns all identities for this Telegram user, newest-first. Ordering by {@code createdAt
     * DESC} means callers always try the most-recently linked workspace first when a user has
     * connected to multiple guest workspaces across sessions.
     */
    @Query(
            """
            select e from ExternalIdentity e
            where e.provider = :provider
              and e.externalUserId = :externalUserId
            order by e.createdAt desc
            """)
    List<ExternalIdentity> findAllForExternalUser(
            @Param("provider") ChannelProvider provider,
            @Param("externalUserId") String externalUserId);
}
