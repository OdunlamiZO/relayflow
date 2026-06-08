package com.relayflow.api.subscription.domain;

/**
 * Supported payment providers. Stored per-plan in Redis under {@code
 * relayflow:plan:{PLAN}:provider}.
 */
public enum PaymentProvider {
    PAYSTACK
}
