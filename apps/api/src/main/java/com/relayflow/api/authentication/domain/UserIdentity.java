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
 * Represents one authentication identity for a user — how they prove who they are.
 *
 * <p>A user may have multiple identities (e.g. email/password and Google OAuth) sharing the same
 * {@link User} record. The {@code (provider, provider_subject)} pair is globally unique.
 */
@Getter
@Setter
@Entity
@Table(name = "user_identities")
public class UserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuthenticationProvider provider;

    /**
     * Provider-specific subject identifier. For EMAIL this is the email address; for GOOGLE it is
     * the OAuth {@code sub} claim; for ANONYMOUS it is a random UUID.
     */
    @Column(name = "provider_subject", nullable = false, length = 200)
    private String providerSubject;

    /**
     * Stored credential. BCrypt password hash for EMAIL identities; {@code null} for OAuth and
     * anonymous identities.
     */
    @Column private String credential;

    /** Whether this identity has been verified (email confirmed, OAuth callback received, etc.). */
    @Column(nullable = false)
    private boolean verified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
