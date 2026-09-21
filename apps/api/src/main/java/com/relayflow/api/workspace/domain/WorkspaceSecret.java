package com.relayflow.api.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * A named, encrypted secret value scoped to a workspace — e.g. a third-party API key a Send HTTP
 * Request node can reference without ever exposing the plaintext value in the workflow definition
 * or run logs.
 */
@Getter
@Setter
@Entity
@Table(name = "workspace_secrets")
public class WorkspaceSecret {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** Referenced in workflows as {@code {{secrets.NAME}}} — see VariableInterpolator. */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** AES-256-GCM encrypted via {@code CredentialEncryptionService}. Never returned by the API. */
    @Column(name = "encrypted_value", nullable = false, columnDefinition = "text")
    private String encryptedValue;

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
