package com.relayflow.api.messaging.domain;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@SQLDelete(sql = "UPDATE external_identities SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(
        name = "external_identities",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "external_identities_workspace_provider_external_user_id_key",
                        columnNames = {"workspace_id", "provider", "external_user_id"}))
public class ExternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ChannelProvider provider;

    @Column(name = "external_user_id", nullable = false, length = 200)
    private String externalUserId;

    @Column(name = "external_conversation_id", length = 200)
    private String externalConversationId;

    @Column(length = 200)
    private String username;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_profile", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawProfile = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (rawProfile == null) {
            rawProfile = new LinkedHashMap<>();
        }
    }
}
