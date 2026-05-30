package com.relayflow.api.messaging.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "workspace_invites")
public class WorkspaceInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(nullable = false, unique = true)
    private UUID token;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "workspace_invite_permissions",
            joinColumns = @JoinColumn(name = "workspace_invite_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", length = 40)
    private Set<WorkspacePermission> permissions = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (token == null) {
            token = UUID.randomUUID();
        }
    }

    public InviteStatus status() {
        Instant now = Instant.now();

        if (acceptedAt != null) {
            return InviteStatus.ACCEPTED;
        }

        if (revokedAt != null) {
            return InviteStatus.REVOKED;
        }

        if (now.isAfter(expiresAt)) {
            return InviteStatus.EXPIRED;
        }

        return InviteStatus.PENDING;
    }

    public enum InviteStatus {
        PENDING,
        ACCEPTED,
        REVOKED,
        EXPIRED,
    }
}
