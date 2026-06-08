package com.relayflow.api.subscription.domain;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * One-to-one with {@link Workspace}. Holds the current billing tier and the payment-provider
 * identifiers needed to manage the subscription.
 *
 * <p>All payment-provider columns are nullable, so the entity is valid for FREE workspaces that
 * have never touched billing. The active provider's webhook handler populates them on upgrade.
 */
@Getter
@Setter
@Entity
@Table(name = "workspace_subscriptions")
public class WorkspaceSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false, unique = true)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Plan plan = Plan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    // ── Payment-provider fields — populated when billing is added ────────────
    // Column names are provider-neutral; the active payment provider populates
    // them via its webhook handler (e.g. Paystack customer_code / subscription_code).

    /**
     * Identifies which billing provider manages this subscription. Null for free workspaces that
     * have never been upgraded.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider", length = 50)
    private PaymentProvider paymentProvider;

    /** Provider-assigned customer identifier. Null for free workspaces. */
    @Column(name = "payment_customer_code", length = 255)
    private String paymentCustomerCode;

    /** Provider-assigned subscription identifier. Null for free workspaces. */
    @Column(name = "payment_subscription_code", length = 255)
    private String paymentSubscriptionCode;

    /**
     * Email token issued by Paystack when the subscription was created. Required to call {@code
     * POST /subscription/disable} for merchant-initiated cancellation.
     */
    @Column(name = "payment_subscription_token", length = 255)
    private String paymentSubscriptionToken;

    /** End of the current billing period. Null for free workspaces. */
    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    /** Number of channel accounts disabled when this subscription was last downgraded to FREE. */
    @Column(name = "downgrade_locked_channels", nullable = false)
    private int downgradeLockedChannels = 0;

    /** Number of workflows disabled when this subscription was last downgraded to FREE. */
    @Column(name = "downgrade_locked_workflows", nullable = false)
    private int downgradeLockedWorkflows = 0;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
