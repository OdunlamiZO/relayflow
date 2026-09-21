package com.relayflow.api.workspace.repository;

import com.relayflow.api.workspace.domain.WorkspaceSecret;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceSecretRepository extends JpaRepository<WorkspaceSecret, UUID> {

    @Query("select s from WorkspaceSecret s where s.workspaceId = :workspaceId order by s.name asc")
    List<WorkspaceSecret> findByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("select s from WorkspaceSecret s where s.id = :id and s.workspaceId = :workspaceId")
    Optional<WorkspaceSecret> findInWorkspace(
            @Param("id") UUID id, @Param("workspaceId") UUID workspaceId);

    Optional<WorkspaceSecret> findByWorkspaceIdAndName(UUID workspaceId, String name);
}
