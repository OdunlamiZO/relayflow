package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.Contact;
import java.time.Instant;
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
    void softDeleteByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);
}
