package com.relayflow.api.webhook;

import com.relayflow.api.webhook.domain.WorkspaceWebhook;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceWebhookRepository extends JpaRepository<WorkspaceWebhook, UUID> {

    @Query("select w from WorkspaceWebhook w where w.workspaceId = :workspaceId")
    Optional<WorkspaceWebhook> findByWorkspace(@Param("workspaceId") UUID workspaceId);
}
