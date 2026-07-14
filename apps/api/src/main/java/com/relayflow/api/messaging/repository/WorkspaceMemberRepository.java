package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.WorkspaceMember;
import com.relayflow.api.messaging.domain.WorkspaceRole;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    @Query("select m from WorkspaceMember m where m.userId = :userId")
    List<WorkspaceMember> findByUser(@Param("userId") UUID userId);

    @Query("select m from WorkspaceMember m where m.workspaceId = :workspaceId")
    List<WorkspaceMember> findByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("select m from WorkspaceMember m where m.workspaceId = :workspaceId and m.id = :id")
    Optional<WorkspaceMember> findInWorkspace(
            @Param("workspaceId") UUID workspaceId, @Param("id") UUID id);

    @Query(
            "select m from WorkspaceMember m where m.workspaceId = :workspaceId and m.userId = :userId")
    Optional<WorkspaceMember> findByWorkspaceAndUser(
            @Param("workspaceId") UUID workspaceId, @Param("userId") UUID userId);

    @Query("select count(m) > 0 from WorkspaceMember m where m.userId = :userId and m.role = :role")
    boolean existsByUserAndRole(@Param("userId") UUID userId, @Param("role") WorkspaceRole role);

    @Modifying
    @Query("UPDATE WorkspaceMember m SET m.deletedAt = :now WHERE m.workspaceId = :workspaceId")
    void softDeleteByWorkspace(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);
}
