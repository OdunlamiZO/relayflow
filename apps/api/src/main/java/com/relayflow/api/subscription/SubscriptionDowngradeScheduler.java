package com.relayflow.api.subscription;

import com.relayflow.api.subscription.BillingProvider.SubscriptionVerification;
import com.relayflow.api.subscription.domain.Plan;
import com.relayflow.api.subscription.domain.SubscriptionStatus;
import com.relayflow.api.subscription.domain.WorkspaceSubscription;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly job that processes subscription downgrades.
 *
 * <p>Handles two cases:
 *
 * <ul>
 *   <li>{@code CANCELLATION_SCHEDULED} — user cancelled; downgrade once {@code currentPeriodEnd}
 *       has passed.
 *   <li>{@code PAST_DUE} — renewal failed; downgrade once the 7-day grace period after {@code
 *       currentPeriodEnd} has passed, but only after confirming with the billing provider that the
 *       subscription is genuinely not active (guards against missed webhooks on a recovered
 *       charge).
 * </ul>
 */
@Component
public class SubscriptionDowngradeScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionDowngradeScheduler.class);

    private final SubscriptionService subscriptionService;

    private final PlanConfigurationService planConfigurationService;

    private final List<BillingProvider> billingProviders;

    public SubscriptionDowngradeScheduler(
            SubscriptionService subscriptionService,
            PlanConfigurationService planConfigurationService,
            List<BillingProvider> billingProviders) {
        this.subscriptionService = subscriptionService;
        this.planConfigurationService = planConfigurationService;
        this.billingProviders = billingProviders;
    }

    @Scheduled(fixedDelay = 3_600_000) // every hour
    public void run() {
        Instant now = Instant.now();
        List<WorkspaceSubscription> due = subscriptionService.findDueForDowngrade(now);

        if (due.isEmpty()) {
            return;
        }

        log.info("Downgrade scheduler: {} subscription(s) due for processing", due.size());

        for (WorkspaceSubscription subscription : due) {
            try {
                process(subscription);
            } catch (Exception e) {
                log.error(
                        "Error processing downgrade for workspaceId={}: {}",
                        subscription.getWorkspace().getId(),
                        e.getMessage(),
                        e);
            }
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void process(WorkspaceSubscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.CANCELLATION_SCHEDULED) {
            downgrade(subscription);
            return;
        }

        // PAST_DUE: verify with the billing provider before downgrading.
        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            if (isGenuinelyFailed(subscription)) {
                downgrade(subscription);
            } else {
                log.info(
                        "Skipping downgrade — billing provider reports subscription is still"
                                + " active: workspaceId={}",
                        subscription.getWorkspace().getId());
            }
        }
    }

    /**
     * Returns {@code true} if the billing provider confirms the subscription is not active. Returns
     * {@code true} conservatively when no provider is configured or the call fails — in that case
     * we trust the {@code PAST_DUE} status set by the webhook.
     */
    private boolean isGenuinelyFailed(WorkspaceSubscription subscription) {
        if (subscription.getPaymentSubscriptionCode() == null) {
            return true;
        }

        BillingProvider provider =
                billingProviders.stream()
                        .filter(p -> p.provider() == subscription.getPaymentProvider())
                        .findFirst()
                        .orElse(null);

        if (provider == null) {
            return true;
        }

        try {
            SubscriptionVerification verification =
                    provider.verify(subscription.getPaymentSubscriptionCode());

            if (verification.active()) {
                // Charge recovered — flip back to ACTIVE with the provider's real next payment
                // date.
                // Fall back to extending by the plan interval if the provider omits the date.
                Instant newPeriodEnd =
                        verification.nextPaymentDate() != null
                                ? verification.nextPaymentDate()
                                : Instant.now().plusSeconds(periodSeconds(subscription.getPlan()));

                subscriptionService.renew(subscription.getWorkspace().getId(), newPeriodEnd, null);
            }

            return !verification.active();

        } catch (Exception e) {
            log.warn(
                    "Error verifying subscription with billing provider for workspaceId={}"
                            + " — proceeding with downgrade: {}",
                    subscription.getWorkspace().getId(),
                    e.getMessage());

            return true;
        }
    }

    private void downgrade(WorkspaceSubscription subscription) {
        subscriptionService.downgrade(subscription.getWorkspace().getId());

        log.info(
                "Downgraded to FREE: workspaceId={}, previousStatus={}",
                subscription.getWorkspace().getId(),
                subscription.getStatus());
    }

    private long periodSeconds(Plan plan) {

        return planConfigurationService.periodSeconds(plan);
    }
}
