package com.relayflow.api.webhook;

import com.relayflow.api.webhook.domain.WorkspaceWebhook;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceWebhookRepository extends JpaRepository<WorkspaceWebhook, UUID> {

    @Query("select w from WorkspaceWebhook w where w.workspaceId = :workspaceId")
    List<WorkspaceWebhook> findByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("select w from WorkspaceWebhook w where w.id = :id and w.workspaceId = :workspaceId")
    Optional<WorkspaceWebhook> findInWorkspace(
            @Param("id") UUID id, @Param("workspaceId") UUID workspaceId);
}
