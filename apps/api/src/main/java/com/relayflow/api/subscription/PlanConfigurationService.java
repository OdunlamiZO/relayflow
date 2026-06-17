package com.relayflow.api.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.subscription.domain.PaymentProvider;
import com.relayflow.api.subscription.domain.Plan;
import com.relayflow.api.subscription.domain.PlanConfiguration;
import com.relayflow.api.subscription.domain.PlanLimits;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Reads and writes per-plan configuration (limits and pricing) from Redis.
 *
 * <p>Redis key schema:
 *
 * <ul>
 *   <li>{@code relayflow:plan:{PLAN}:config} → JSON {@link PlanConfiguration} (limits and pricing)
 *   <li>{@code relayflow:plan:{PLAN}:provider} → {@link PaymentProvider} name (e.g. {@code
 *       PAYSTACK}); defaults to PAYSTACK when absent
 *   <li>{@code relayflow:paystack:plan:{PLAN}:code} → Paystack recurring plan code (e.g. {@code
 *       PLN_xxx})
 * </ul>
 *
 * <p>When a Redis key is absent or Redis is unavailable, the service falls back to sensible
 * defaults so the application continues to function without a live Redis connection during
 * development.
 *
 * <p>Updating a plan takes effect immediately without a restart:
 *
 * <pre>
 *   SET relayflow:plan:PRO:config '{"maxChannelAccounts":2147483647,"maxWorkflows":2147483647,"priceNgn":5000,"billingInterval":"monthly"}'
 *   SET relayflow:plan:PRO:provider 'PAYSTACK'
 *   SET relayflow:paystack:plan:PRO:code 'PLN_xxx'
 * </pre>
 */
@Service
public class PlanConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(PlanConfigurationService.class);

    private static final String CONFIG_KEY_TEMPLATE = "relayflow:plan:%s:config";

    private static final String PROVIDER_KEY_TEMPLATE = "relayflow:plan:%s:provider";

    private static final String PAYSTACK_CODE_KEY_TEMPLATE = "relayflow:paystack:plan:%s:code";

    private final StringRedisTemplate redis;

    private final ObjectMapper objectMapper;

    public PlanConfigurationService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Returns the current configuration for the given plan. Reads from Redis on every call — no
     * in-process cache — so changes take effect within milliseconds.
     *
     * @throws IllegalStateException if the key is absent or Redis is unavailable — plan
     *     configuration must be seeded before the API handles requests (see README).
     */
    public PlanConfiguration getConfiguration(Plan plan) {
        String key = configKey(plan);

        try {
            String json = redis.opsForValue().get(key);

            if (json != null && !json.isBlank()) {
                return objectMapper.readValue(json, PlanConfiguration.class);
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read plan configuration from Redis (key=" + key + ")", e);
        }

        throw new IllegalStateException(
                "No plan configuration found in Redis for plan "
                        + plan
                        + " (key="
                        + key
                        + "). Run the redis-cli SET commands from the README before starting the API.");
    }

    /**
     * Returns {@code true} if at least one paid plan is currently purchasable.
     *
     * <p>Used by {@link SubscriptionService} to populate {@code upgradeAvailable} on the workspace
     * subscription response.
     */
    public boolean isUpgradeAvailable() {

        return Arrays.stream(Plan.values())
                .filter(plan -> plan != Plan.FREE)
                .anyMatch(this::isPurchasable);
    }

    /**
     * Returns {@code true} if the given plan is configured and ready to sell.
     *
     * <p>A plan is purchasable when its payment provider is configured, and all provider-specific
     * prerequisites (e.g., a Paystack plan code) are present in Redis. The FREE plan is never
     * purchasable — it is assigned automatically on signup.
     *
     * <p>Used by the plan catalogue endpoint to set {@code upgradeAvailable} per plan entry.
     */
    public boolean isPurchasable(Plan plan) {
        if (plan == Plan.FREE) {
            return false;
        }

        PaymentProvider provider = getProvider(plan);

        return switch (provider) {
            case PAYSTACK -> getPaystackPlanCode(plan) != null;
        };
    }

    /**
     * Returns the active {@link PaymentProvider} for the given plan.
     *
     * <p>Redis key: {@code relayflow:plan:{PLAN}:provider}. Defaults to {@link
     * PaymentProvider#PAYSTACK} when the key is absent so existing setups keep working without any
     * extra Redis writes.
     */
    public PaymentProvider getProvider(Plan plan) {
        String key = providerKey(plan);

        try {
            String value = redis.opsForValue().get(key);

            if (value != null && !value.isBlank()) {
                return PaymentProvider.valueOf(value.strip().toUpperCase());
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to read payment provider from Redis (key={}) — defaulting to PAYSTACK: {}",
                    key,
                    e.getMessage());
        }

        return PaymentProvider.PAYSTACK;
    }

    /**
     * Returns the Paystack plan code for the given plan, or {@code null} if not configured.
     *
     * <p>Redis key: {@code relayflow:paystack:plan:{PLAN}:code}
     */
    public String getPaystackPlanCode(Plan plan) {
        String key = paystackCodeKey(plan);

        try {
            String code = redis.opsForValue().get(key);

            return (code != null && !code.isBlank()) ? code.strip() : null;
        } catch (Exception e) {
            log.warn(
                    "Failed to read Paystack plan code from Redis (key={}) — returning null: {}",
                    key,
                    e.getMessage());

            return null;
        }
    }

    /** Returns only the limit portion of the plan configuration. */
    public PlanLimits getLimits(Plan plan) {

        return getConfiguration(plan).toLimits();
    }

    /**
     * Returns the billing period length in seconds for the given plan, derived from its configured
     * {@link com.relayflow.api.subscription.domain.BillingInterval}.
     *
     * @throws IllegalStateException if the plan configuration is absent or unreadable — callers
     *     should let this propagate so the failure is visible rather than silently using a wrong
     *     period length.
     */
    public long periodSeconds(Plan plan) {
        if (plan == Plan.FREE) {
            throw new IllegalArgumentException("FREE plan has no billing period.");
        }

        var interval = getConfiguration(plan).billingInterval();

        if (interval == null) {
            throw new IllegalStateException(
                    "Plan "
                            + plan
                            + " has no billingInterval configured — cannot compute period length.");
        }

        return switch (interval) {
            case ANNUAL -> 365L * 24 * 3600;
            case MONTHLY -> 30L * 24 * 3600;
        };
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private static String configKey(Plan plan) {

        return String.format(CONFIG_KEY_TEMPLATE, plan.name());
    }

    private static String providerKey(Plan plan) {

        return String.format(PROVIDER_KEY_TEMPLATE, plan.name());
    }

    private static String paystackCodeKey(Plan plan) {

        return String.format(PAYSTACK_CODE_KEY_TEMPLATE, plan.name());
    }
}
