package com.relayflow.api.subscription.domain;

/**
 * Value object holding the numeric resource limits for a plan.
 *
 * <p>Instances are built by {@link PlanConfigurationService} from Redis — there are no hardcoded
 * constants here. Use {@link Integer#MAX_VALUE} to represent "unlimited"; the helper {@link
 * #isUnlimited(int)} encapsulates that convention.
 */
public record PlanLimits(int maxChannelAccounts, int maxWorkflows, int maxMembersPerWorkspace) {

    private static final int UNLIMITED = Integer.MAX_VALUE;

    /** Returns {@code true} if the given limit value represents "unlimited". */
    public static boolean isUnlimited(int limit) {

        return limit == UNLIMITED;
    }
}
