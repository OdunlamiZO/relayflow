package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.WorkspaceApiKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceApiKeyRepository extends JpaRepository<WorkspaceApiKey, UUID> {

    List<WorkspaceApiKey> findByWorkspaceIdAndRevokedAtIsNullOrderByCreatedAtDesc(UUID workspaceId);

    Optional<WorkspaceApiKey> findByKeyHash(String keyHash);

    Optional<WorkspaceApiKey> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
