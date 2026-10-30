package io.odunlamizo.relayflow.contact.repository;

// Docs: docs/codebase-components.md#contact-repositories

import io.odunlamizo.relayflow.channel.domain.ChannelProvider;
import io.odunlamizo.relayflow.contact.domain.Contact;
import io.odunlamizo.relayflow.contact.domain.ExternalIdentity;
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
    void softDeleteByWorkspace(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE ExternalIdentity e SET e.deletedAt = :now WHERE e.contact.id = :contactId")
    void softDeleteByContact(@Param("contactId") UUID contactId, @Param("now") Instant now);

    /** Unique per channel account, so one Telegram user on two bots has two identities. */
    @Query(
            """
            select e from ExternalIdentity e
            where e.channelAccount.id = :channelAccountId
              and e.externalUserId = :externalUserId
            """)
    Optional<ExternalIdentity> findForExternalUser(
            @Param("channelAccountId") UUID channelAccountId,
            @Param("externalUserId") String externalUserId);

    @Query(
            """
            select e from ExternalIdentity e
            where e.channelAccount.id = :channelAccountId
              and e.contact.id = :contactId
            """)
    Optional<ExternalIdentity> findForContact(
            @Param("channelAccountId") UUID channelAccountId, @Param("contactId") UUID contactId);

    @Query(
            """
            select e from ExternalIdentity e
            where e.contact.id = :contactId
              and e.provider = :provider
            """)
    Optional<ExternalIdentity> findForContactOnProvider(
            @Param("contactId") UUID contactId, @Param("provider") ChannelProvider provider);

    @Query(
            "select e from ExternalIdentity e where e.contact.id = :contactId order by e.createdAt asc")
    List<ExternalIdentity> findByContact(@Param("contactId") UUID contactId);

    @Query(
            "select e from ExternalIdentity e where e.contact.id in :contactIds order by e.createdAt asc")
    List<ExternalIdentity> findByContacts(@Param("contactIds") Collection<UUID> contactIds);

    @Modifying
    @Query("UPDATE ExternalIdentity e SET e.contact = :target WHERE e.contact.id = :sourceId")
    void reassignContact(@Param("target") Contact target, @Param("sourceId") UUID sourceId);
}
