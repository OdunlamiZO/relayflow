package com.relayflow.api.subscription.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Billing cadence for a paid plan. Serializes to/from lowercase strings for Redis and the API. */
public enum BillingInterval {
    @JsonProperty("monthly")
    MONTHLY,

    @JsonProperty("annual")
    ANNUAL,
}
