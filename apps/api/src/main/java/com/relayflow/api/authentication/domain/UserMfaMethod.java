package com.relayflow.api.authentication.domain;

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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * One second-factor authentication method belonging to a user.
 *
 * <p>A unique constraint on {@code (user_id, type)} ensures at most one active record per method
 * type. Adding SMS in the future is a new row with {@code type = SMS} — no schema change.
 */
@Getter
@Setter
@Entity
@Table(name = "user_mfa_methods")
public class UserMfaMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MfaMethodType type;

    /**
     * Encrypted TOTP base32 secret for {@link MfaMethodType#TOTP}; verified phone number for {@link
     * MfaMethodType#SMS}; {@code null} until setup is confirmed.
     */
    @Column private String credential;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
