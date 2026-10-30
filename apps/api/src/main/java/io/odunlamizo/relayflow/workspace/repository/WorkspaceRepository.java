package io.odunlamizo.relayflow.workspace.repository;

// Docs: docs/codebase-components.md#workspace-repositories

import io.odunlamizo.relayflow.workspace.domain.Workspace;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {}
