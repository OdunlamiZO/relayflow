package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.WorkspaceMember;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    List<WorkspaceMember> findByUserId(UUID userId);

    @Modifying
    @Query("UPDATE WorkspaceMember m SET m.deletedAt = :now WHERE m.workspaceId = :workspaceId")
    void softDeleteByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);
}
