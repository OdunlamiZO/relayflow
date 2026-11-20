package io.odunlamizo.relayflow.knowledge.domain;

// Docs: docs/codebase-components.md#knowledgesource

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A GET endpoint whose JSON values an AI agent's knowledge base can include. */
@Getter
@Setter
@Entity
@Table(name = "knowledge_sources")
public class KnowledgeSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "url", nullable = false, columnDefinition = "text")
    private String url;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", nullable = false, columnDefinition = "jsonb")
    private List<KnowledgeSourceHeader> headers = new ArrayList<>();

    @Column(name = "cache_seconds", nullable = false)
    private int cacheSeconds = 60;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
