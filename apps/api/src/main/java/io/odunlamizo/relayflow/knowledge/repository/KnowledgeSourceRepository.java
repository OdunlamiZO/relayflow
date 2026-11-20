package io.odunlamizo.relayflow.knowledge.repository;

// Docs: docs/codebase-components.md#knowledgesourcerepository

import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSource;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSource, UUID> {

    @Query("select s from KnowledgeSource s where s.workspaceId = :workspaceId order by s.name asc")
    List<KnowledgeSource> findByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("select s from KnowledgeSource s where s.id = :id and s.workspaceId = :workspaceId")
    Optional<KnowledgeSource> findInWorkspace(
            @Param("id") UUID id, @Param("workspaceId") UUID workspaceId);

    @Query(
            "select s from KnowledgeSource s where s.workspaceId = :workspaceId and s.name in"
                    + " :names")
    List<KnowledgeSource> findByWorkspaceAndNames(
            @Param("workspaceId") UUID workspaceId, @Param("names") Collection<String> names);

    @Query(
            value =
                    "select name from ai_agent_configs where workspace_id = :workspaceId and"
                            + " (knowledge_base::text like concat('%{{sources.', :sourceName, '.%')"
                            + " or knowledge_base::text like concat('%{{sources.', :sourceName,"
                            + " '[%')"
                            + " or knowledge_base::text like concat('%{{sources.', :sourceName,"
                            + " '}}%'))"
                            + " order by name",
            nativeQuery = true)
    List<String> findAiAgentNamesReferencing(
            @Param("workspaceId") UUID workspaceId, @Param("sourceName") String sourceName);
}
