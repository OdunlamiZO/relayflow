package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.ChannelProvider;
import com.relayflow.api.messaging.domain.Contact;
import com.relayflow.api.messaging.domain.ExternalIdentity;
import java.time.Instant;
import java.util.Collection;
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

    /**
     * Finds the identity for a given external user on a specific channel account (bot). Uniqueness
     * is enforced at the channel-account level, not just the provider level — the same Telegram
     * user ID on two different bots yields two separate identities.
     */
    @Query(
            """
            select e from ExternalIdentity e
            where e.channelAccount.id = :channelAccountId
              and e.externalUserId = :externalUserId
            """)
    Optional<ExternalIdentity> findForExternalUser(
            @Param("channelAccountId") UUID channelAccountId,
            @Param("externalUserId") String externalUserId);

    /** Finds the identity used for outbound relay on a specific channel account and contact. */
    @Query(
            """
            select e from ExternalIdentity e
            where e.channelAccount.id = :channelAccountId
              and e.contact.id = :contactId
            """)
    Optional<ExternalIdentity> findForContact(
            @Param("channelAccountId") UUID channelAccountId, @Param("contactId") UUID contactId);

    /**
     * Returns the identity for a contact on a given provider, regardless of workspace. Used by the
     * workflow engine to resolve the contact's external user ID and username when building the
     * execution context.
     */
    @Query(
            """
            select e from ExternalIdentity e
            where e.contact.id = :contactId
              and e.provider = :provider
            """)
    Optional<ExternalIdentity> findForContactOnProvider(
            @Param("contactId") UUID contactId, @Param("provider") ChannelProvider provider);

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

    @Query(
            "select e from ExternalIdentity e where e.contact.id = :contactId order by e.createdAt asc")
    List<ExternalIdentity> findByContactId(@Param("contactId") UUID contactId);

    @Query(
            "select e from ExternalIdentity e where e.contact.id in :contactIds order by e.createdAt asc")
    List<ExternalIdentity> findByContactIds(@Param("contactIds") Collection<UUID> contactIds);

    @Modifying
    @Query("UPDATE ExternalIdentity e SET e.contact = :target WHERE e.contact.id = :sourceId")
    void reassignContact(@Param("target") Contact target, @Param("sourceId") UUID sourceId);
}
