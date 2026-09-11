package com.relayflow.api.messaging.domain;

import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.workspace.domain.Workspace;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "conversations")
@SQLDelete(sql = "UPDATE conversations SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_account_id", nullable = false)
    private ChannelAccount channelAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ConversationStatus status = ConversationStatus.OPEN;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "locked_by_workflow", nullable = false)
    private boolean lockedByWorkflow = false;

    @Column(name = "locked_by_ai_agent", nullable = false)
    private boolean lockedByAiAgent = false;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    /** Set when the AI agent escalates; cleared when a human agent sends the next reply. */
    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "escalation_reason", length = 255)
    private String escalationReason;

    @Column(name = "session_started_at", nullable = false)
    private Instant sessionStartedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (sessionStartedAt == null) {
            sessionStartedAt = createdAt;
        }
        if (status == null) {
            status = ConversationStatus.OPEN;
        }
    }
}
