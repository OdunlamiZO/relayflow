package com.relayflow.api.subscription.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * Full configuration for a billing plan — resource limits AND pricing.
 *
 * <p>Instances are stored as JSON in Redis under {@code relayflow:plan:{PLAN_NAME}:config} and must
 * be set before the API starts. See the README for the required {@code redis-cli SET} commands.
 *
 * <p>Resource limits: use {@link Integer#MAX_VALUE} to mean "unlimited".
 *
 * <p>Payment-provider-specific identifiers (e.g., Paystack plan codes) are stored under separate
 * Redis keys and are NOT part of this record, keeping it provider-neutral.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanConfiguration(

        // ── Limits ──────────────────────────────────────────────────────────

        int maxChannelAccounts,
        int maxWorkflows,
        int maxMembersPerWorkspace,

        // ── Pricing ─────────────────────────────────────────────────────────

        BigDecimal priceNgn,
        BillingInterval billingInterval) {

    /** Builds a {@link PlanLimits} from this configuration's limit fields. */
    public PlanLimits toLimits() {

        return new PlanLimits(maxChannelAccounts, maxWorkflows, maxMembersPerWorkspace);
    }
}
