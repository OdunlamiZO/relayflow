package com.relayflow.api.subscription.domain;

/** Lifecycle state of a workspace's subscription. */
public enum SubscriptionStatus {
    ACTIVE,

    /** User requested cancellation. PRO access continues until {@code currentPeriodEnd}. */
    CANCELLATION_SCHEDULED,

    PAST_DUE,
    CANCELLED
}
