package com.relayflow.api.subscription.paystack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The {@code data} field inside Paystack {@code subscription.create}, {@code subscription.disable},
 * and {@code subscription.not_renew} webhook events.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaystackSubscriptionData(
        @JsonProperty("subscription_code") String subscriptionCode,

        /* Next renewal date — ISO 8601 string. Present on {@code subscription.create}. */
        @JsonProperty("next_payment_date") String nextPaymentDate,

        /*
         * Email token issued by Paystack on subscription creation. Stored so the
         * merchant can call POST /subscription/disable for user-initiated cancellation.
         * Present on {@code subscription.create}; absent on {@code subscription.disable}.
         */
        @JsonProperty("email_token") String emailToken,

        /*
         * Customer who owns the subscription. Used as a fallback lookup key in
         * {@code subscription.create} when {@code charge.success} activated the workspace
         * without a subscription code (Paystack creates the subscription asynchronously).
         */
        Customer customer) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Customer(@JsonProperty("customer_code") String customerCode) {}
}
