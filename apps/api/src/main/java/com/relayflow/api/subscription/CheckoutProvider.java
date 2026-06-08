package com.relayflow.api.subscription;

import com.relayflow.api.subscription.domain.PaymentProvider;
import com.relayflow.api.subscription.domain.Plan;
import com.relayflow.api.subscription.domain.PlanConfiguration;
import java.util.UUID;

/**
 * Strategy interface for payment-provider-specific checkout logic.
 *
 * <p>Implement this interface for each supported payment provider and annotate the implementation
 * with {@code @Component}. {@link SubscriptionCheckoutService} discovers all implementations at
 * startup and dispatches to the one matching the plan's configured {@link PaymentProvider}.
 */
public interface CheckoutProvider {

    /** Returns the {@link PaymentProvider} this implementation handles. */
    PaymentProvider provider();

    /**
     * Initializes a checkout session for the given workspace and plan.
     *
     * @param workspaceId workspace being upgraded
     * @param plan target plan
     * @param ownerEmail email of the workspace owner (used as the billing contact)
     * @param configuration current plan configuration (limits and pricing)
     * @return authorization URL to redirect the user to
     */
    String initializeCheckout(
            UUID workspaceId, Plan plan, String ownerEmail, PlanConfiguration configuration);
}
