package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.Workspace;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {}
