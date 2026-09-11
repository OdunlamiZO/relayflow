package com.relayflow.api.workspace.repository;

import com.relayflow.api.workspace.domain.WorkspaceInvite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceInviteRepository extends JpaRepository<WorkspaceInvite, UUID> {

    Optional<WorkspaceInvite> findByToken(UUID token);

    /** Pending = not accepted and not revoked (may still be expired — service checks expiry). */
    @Query(
            """
            SELECT i FROM WorkspaceInvite i
            WHERE i.workspaceId = :workspaceId
              AND i.acceptedAt IS NULL
              AND i.revokedAt  IS NULL
            ORDER BY i.createdAt DESC
            """)
    List<WorkspaceInvite> findPending(@Param("workspaceId") UUID workspaceId);

    /** Find any non-accepted, non-revoked invite for the given email in the workspace. */
    @Query(
            """
            SELECT i FROM WorkspaceInvite i
            WHERE i.workspaceId  = :workspaceId
              AND LOWER(i.email) = LOWER(:email)
              AND i.acceptedAt   IS NULL
              AND i.revokedAt    IS NULL
            """)
    Optional<WorkspaceInvite> findActivePendingByWorkspaceAndEmail(
            @Param("workspaceId") UUID workspaceId, @Param("email") String email);

    @Query("SELECT i FROM WorkspaceInvite i WHERE i.workspaceId = :workspaceId AND i.id = :id")
    Optional<WorkspaceInvite> findInWorkspace(
            @Param("workspaceId") UUID workspaceId, @Param("id") UUID id);
}
