package com.relayflow.api.subscription.paystack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** The {@code data} field inside a Paystack {@code charge.success} webhook event. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaystackChargeData(
        String reference,

        /*
         Paystack sends {@code plan} as a nested object, not a bare string.
         Only {@code plan_code} is needed to resolve the RelayFlow plan.
        */
        PaystackPlanRef plan,
        @JsonProperty("subscription_code") String subscriptionCode,
        Customer customer,

        /*
         Custom metadata passed during transaction initialization. RelayFlow encodes
         {@code workspace_id} here to identify which workspace to activate.
        */
        Map<String, Object> metadata) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaystackPlanRef(@JsonProperty("plan_code") String planCode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Customer(String email, @JsonProperty("customer_code") String customerCode) {}
}
