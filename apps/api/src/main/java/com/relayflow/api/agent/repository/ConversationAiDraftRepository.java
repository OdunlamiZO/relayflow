package com.relayflow.api.agent.repository;

import com.relayflow.api.agent.domain.ConversationAiDraft;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationAiDraftRepository extends JpaRepository<ConversationAiDraft, UUID> {

    @Query("select d from ConversationAiDraft d where d.conversation.id = :conversationId")
    Optional<ConversationAiDraft> findByConversation(@Param("conversationId") UUID conversationId);

    @Modifying
    @Query("delete from ConversationAiDraft d where d.conversation.id = :conversationId")
    void deleteByConversation(@Param("conversationId") UUID conversationId);

    @Modifying
    @Query("delete from ConversationAiDraft d where d.conversation.id in :conversationIds")
    void deleteByConversations(@Param("conversationIds") Collection<UUID> conversationIds);

    @Modifying
    @Query("delete from ConversationAiDraft d where d.workspace.id = :workspaceId")
    void deleteByWorkspace(@Param("workspaceId") UUID workspaceId);
}
