package com.relayflow.api.agent.domain;

import com.relayflow.api.messaging.domain.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

@Getter
@Setter
@Entity
@Table(name = "ai_agent_configs")
public class AiAgentConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, length = 100)
    private String name = "AI Agent";

    @Column(nullable = false)
    private boolean enabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "autonomy_ceiling", nullable = false, length = 40)
    private AutonomyCeiling autonomyCeiling = AutonomyCeiling.DRAFT_ONLY;

    @Column(columnDefinition = "text")
    private String instructions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "knowledge_base", nullable = false, columnDefinition = "jsonb")
    private List<KnowledgeEntry> knowledgeBase = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "escalation_keywords", nullable = false, columnDefinition = "jsonb")
    private List<String> escalationKeywords = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "workflow_mappings", nullable = false, columnDefinition = "jsonb")
    private List<WorkflowMapping> workflowMappings = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
