package com.relayflow.api.subscription.dto;

import com.relayflow.api.subscription.domain.BillingInterval;
import com.relayflow.api.subscription.domain.Plan;
import com.relayflow.api.subscription.domain.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Current subscription state and plan configuration for a workspace.
 *
 * <p>Limit and price fields use {@code null} to mean "unlimited" or "free" so the frontend can
 * render "∞" or "Free" rather than a large number.
 */
public record SubscriptionResponse(
        Plan plan,
        SubscriptionStatus status,

        /** Max channel accounts allowed. {@code null} = unlimited. */
        Integer maxChannelAccounts,

        /** Max workflow definitions allowed. {@code null} = unlimited. */
        Integer maxWorkflows,

        /** Max workspace members allowed. {@code null} = unlimited. */
        Integer maxMembersPerWorkspace,

        /** End of the current billing period. {@code null} for the FREE plan. */
        Instant currentPeriodEnd,

        /** Monthly price in Nigerian Naira. {@code null} for the FREE plan. */
        BigDecimal priceNgn,

        /** Billing interval. {@code null} for FREE. */
        BillingInterval billingInterval,

        /**
         * {@code true} when there is a paid plan this workspace can upgrade to and payment is
         * configured.
         */
        boolean upgradeAvailable,

        /** {@code true} only when the workspace is on FREE and {@code upgradeAvailable} is true. */
        boolean upgradeRecommended,

        /**
         * Number of channel accounts disabled when the subscription last downgraded to FREE. {@code
         * null} when there is no downgrade notice to show.
         */
        Integer downgradeLockedChannels,

        /**
         * Number of workflows disabled when the subscription last downgraded to FREE. {@code null}
         * when there is no downgrade notice to show.
         */
        Integer downgradeLockedWorkflows) {}
