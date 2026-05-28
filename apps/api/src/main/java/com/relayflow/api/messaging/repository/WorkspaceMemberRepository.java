package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.WorkspaceMember;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    List<WorkspaceMember> findByUserId(UUID userId);

    void deleteByWorkspaceId(UUID workspaceId);
}
