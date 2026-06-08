package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.WorkspaceApiKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceApiKeyRepository extends JpaRepository<WorkspaceApiKey, UUID> {

    @Query(
            "select k from WorkspaceApiKey k where k.workspaceId = :workspaceId and k.revokedAt is null order by k.createdAt desc")
    List<WorkspaceApiKey> findActive(@Param("workspaceId") UUID workspaceId);

    Optional<WorkspaceApiKey> findByKeyHash(String keyHash);

    @Query("select k from WorkspaceApiKey k where k.id = :id and k.workspaceId = :workspaceId")
    Optional<WorkspaceApiKey> findInWorkspace(
            @Param("id") UUID id, @Param("workspaceId") UUID workspaceId);
}
