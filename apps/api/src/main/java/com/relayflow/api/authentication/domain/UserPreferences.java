package com.relayflow.api.authentication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Per-user preferences stored in a dedicated table rather than polluting {@link User}.
 *
 * <p>Uses a shared primary key ({@code user_id}) via {@link MapsId} so there is always exactly one
 * preferences row per user and lookups never need a separate FK index.
 */
@Getter
@Setter
@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "receive_email_updates", nullable = false)
    private boolean receiveEmailUpdates = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
