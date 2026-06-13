package com.relayflow.api.subscription;

import com.relayflow.api.subscription.domain.Plan;
import com.relayflow.api.subscription.domain.PlanConfiguration;
import com.relayflow.api.subscription.domain.PlanLimits;
import com.relayflow.api.subscription.dto.PlanInfo;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public plan catalogue endpoint.
 *
 * <p>Returns all known plans with their current limits, pricing, and availability. No
 * authentication required — used by the landing page and the upgrade modal.
 */
@RestController
@RequestMapping("/plans")
public class PlanController {

    private final PlanConfigurationService planConfigurationService;

    public PlanController(PlanConfigurationService planConfigurationService) {
        this.planConfigurationService = planConfigurationService;
    }

    /** Returns all plans with live limits, pricing, and purchase availability from Redis. */
    @GetMapping
    public List<PlanInfo> getPlans() {

        return Arrays.stream(Plan.values()).map(this::toPlanInfo).toList();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private PlanInfo toPlanInfo(Plan plan) {
        PlanConfiguration configuration = planConfigurationService.getConfiguration(plan);
        PlanLimits limits = configuration.toLimits();

        return new PlanInfo(
                plan,
                PlanLimits.isUnlimited(limits.maxChannelAccounts())
                        ? null
                        : limits.maxChannelAccounts(),
                PlanLimits.isUnlimited(limits.maxWorkflows()) ? null : limits.maxWorkflows(),
                PlanLimits.isUnlimited(limits.maxMembersPerWorkspace())
                        ? null
                        : limits.maxMembersPerWorkspace(),
                configuration.priceNgn().compareTo(java.math.BigDecimal.ZERO) > 0
                        ? configuration.priceNgn()
                        : null,
                configuration.billingInterval(),
                planConfigurationService.isPurchasable(plan));
    }
}
