package com.relayflow.api.agent.repository;

import com.relayflow.api.agent.domain.ConversationAiDraft;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationAiDraftRepository extends JpaRepository<ConversationAiDraft, UUID> {

    Optional<ConversationAiDraft> findByConversationId(UUID conversationId);

    void deleteByConversationId(UUID conversationId);

    void deleteByWorkspaceId(UUID workspaceId);
}
