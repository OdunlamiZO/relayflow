package com.relayflow.api.subscription;

import com.relayflow.api.subscription.domain.PaymentProvider;
import java.io.IOException;
import java.time.Instant;

/**
 * Provider-agnostic interface for subscription billing operations.
 *
 * <p>Implementations are registered as Spring beans. The scheduler and service inject {@code
 * List<BillingProvider>} and look up the right implementation by matching {@link #provider()}
 * against the {@code paymentProvider} stored on each subscription — multiple providers can
 * therefore coexist in the same deployment.
 */
public interface BillingProvider {

    /** Returns the {@link PaymentProvider} constant that identifies this implementation. */
    PaymentProvider provider();

    /**
     * Checks whether the subscription is currently active at the provider.
     *
     * @param subscriptionCode the provider's subscription identifier
     * @return verification result containing the active flag and next payment date (if known)
     * @throws IOException if the provider cannot be reached
     */
    SubscriptionVerification verify(String subscriptionCode) throws IOException;

    /**
     * Instructs the provider to stop future charges for the subscription.
     *
     * @param subscriptionCode the provider's subscription identifier
     * @param emailToken the provider-specific token required to authorise the cancellation
     * @throws IOException if the provider cannot be reached or rejects the request
     */
    void cancel(String subscriptionCode, String emailToken) throws IOException;

    /** Result of a {@link #verify} call. */
    record SubscriptionVerification(boolean active, Instant nextPaymentDate, String emailToken) {}
}
