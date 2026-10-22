package io.odunlamizo.relayflow.workflow.domain;

import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "workflow_runs")
public class WorkflowRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private WorkflowDefinition workflowDefinition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowRunStatus status = WorkflowRunStatus.RUNNING;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "waiting_at_node_id", length = 255)
    private String waitingAtNodeId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Invalid replies at the current Ask Question node. */
    @Column(name = "reply_attempts", nullable = false)
    private int replyAttempts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> contextSnapshot;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowRunStep> steps = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }
}
