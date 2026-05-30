package com.relayflow.api.webhook;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceWebhookRepository extends JpaRepository<WorkspaceWebhook, UUID> {

    Optional<WorkspaceWebhook> findByWorkspaceId(UUID workspaceId);
}
