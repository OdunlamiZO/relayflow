package com.relayflow.api.subscription.dto;

import com.relayflow.api.subscription.domain.BillingInterval;
import com.relayflow.api.subscription.domain.Plan;
import java.math.BigDecimal;

/**
 * Public plan catalogue entry — returned by {@code GET /plans}.
 *
 * <p>Limit fields use {@code null} to mean "unlimited" so the frontend can render "∞". Price is
 * {@code null} for the FREE plan. {@code upgradeAvailable} tells the frontend whether this plan can
 * be purchased right now (provider configured + plan code set in Redis).
 */
public record PlanInfo(
        Plan plan,

        /* Max channel accounts allowed. {@code null} = unlimited. */
        Integer maxChannelAccounts,

        /* Max workflow definitions allowed. {@code null} = unlimited. */
        Integer maxWorkflows,

        /* Max workspace members allowed. {@code null} = unlimited. */
        Integer maxMembersPerWorkspace,

        /* Monthly price in Nigerian Naira. {@code null} for the FREE plan. */
        BigDecimal priceNgn,

        /* Billing interval. {@code null} for FREE. */
        BillingInterval billingInterval,

        /*
         {@code true} when this plan is currently available for purchase. Always {@code false} for
         FREE. The frontend uses this to show "Upgrade" vs "Coming soon".
        */
        boolean upgradeAvailable) {}
