package com.relayflow.api.subscription;

import com.relayflow.api.subscription.domain.LimitType;
import com.relayflow.api.subscription.domain.Plan;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when a workspace would exceed the resource limit for its current plan. Maps to HTTP 402
 * Payment Required so the client knows this is a plan-gate rather than a validation error.
 */
@Getter
public class PlanLimitExceededException extends ResponseStatusException {

    private final Plan plan;

    private final LimitType limitType;

    private final int limit;

    public PlanLimitExceededException(Plan plan, LimitType limitType, int limit) {
        super(
                HttpStatus.PAYMENT_REQUIRED,
                "You've reached the "
                        + limitType.label()
                        + " limit ("
                        + limit
                        + ") for the "
                        + plan.name()
                        + " plan. Upgrade to add more.");
        this.plan = plan;
        this.limitType = limitType;
        this.limit = limit;
    }
}
