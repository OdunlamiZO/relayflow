package com.relayflow.api.contact.repository;

import com.relayflow.api.contact.domain.Contact;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    @Query("select c from Contact c where c.workspace.id = :workspaceId order by c.createdAt desc")
    List<Contact> findByWorkspace(@Param("workspaceId") UUID workspaceId, Pageable pageable);

    @Modifying
    @Query("UPDATE Contact c SET c.deletedAt = :now WHERE c.workspace.id = :workspaceId")
    void softDeleteByWorkspace(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);

    /**
     * Hard-deletes contacts that have no conversations and no external identities left — used to
     * clean up contacts that existed only on a channel account that was just removed.
     */
    @Modifying
    @Query(
            """
            delete from Contact c where c.id in :contactIds
              and not exists (select 1 from Conversation conv where conv.contact = c)
              and not exists (select 1 from ExternalIdentity e where e.contact = c)
            """)
    void deleteOrphaned(@Param("contactIds") Collection<UUID> contactIds);
}
