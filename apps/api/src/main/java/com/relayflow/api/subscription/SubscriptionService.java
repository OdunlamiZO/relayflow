package com.relayflow.api.subscription;

import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.email.EmailService;
import com.relayflow.api.messaging.domain.ChannelAccount;
import com.relayflow.api.messaging.domain.ChannelAccountStatus;
import com.relayflow.api.messaging.domain.Workspace;
import com.relayflow.api.messaging.domain.WorkspaceRole;
import com.relayflow.api.messaging.repository.ChannelAccountRepository;
import com.relayflow.api.messaging.repository.WorkspaceMemberRepository;
import com.relayflow.api.subscription.domain.LimitType;
import com.relayflow.api.subscription.domain.PaymentProvider;
import com.relayflow.api.subscription.domain.Plan;
import com.relayflow.api.subscription.domain.PlanConfiguration;
import com.relayflow.api.subscription.domain.PlanLimits;
import com.relayflow.api.subscription.domain.SubscriptionStatus;
import com.relayflow.api.subscription.domain.WorkspaceSubscription;
import com.relayflow.api.subscription.dto.SubscriptionResponse;
import com.relayflow.api.subscription.repository.WorkspaceSubscriptionRepository;
import com.relayflow.api.workflow.domain.WorkflowDefinition;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final WorkspaceSubscriptionRepository subscriptionRepository;

    private final PlanConfigurationService planConfigurationService;

    private final ChannelAccountRepository channelAccountRepository;

    private final WorkflowDefinitionRepository workflowRepository;

    private final WorkspaceMemberRepository memberRepository;

    private final UserRepository userRepository;

    private final EmailService emailService;

    private final List<BillingProvider> billingProviders;

    public SubscriptionService(
            WorkspaceSubscriptionRepository subscriptionRepository,
            PlanConfigurationService planConfigurationService,
            ChannelAccountRepository channelAccountRepository,
            WorkflowDefinitionRepository workflowRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository,
            EmailService emailService,
            List<BillingProvider> billingProviders) {
        this.subscriptionRepository = subscriptionRepository;
        this.planConfigurationService = planConfigurationService;
        this.channelAccountRepository = channelAccountRepository;
        this.workflowRepository = workflowRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.billingProviders = billingProviders;
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    /** Builds the API response DTO for the subscription endpoint. */
    public SubscriptionResponse getResponse(UUID workspaceId) {
        WorkspaceSubscription subscription = find(workspaceId);
        PlanConfiguration configuration =
                planConfigurationService.getConfiguration(subscription.getPlan());
        PlanLimits limits = configuration.toLimits();

        boolean upgradeAvailable =
                planConfigurationService.isUpgradeAvailable()
                        && subscription.getStatus() != SubscriptionStatus.CANCELLATION_SCHEDULED
                        && memberRepository.countByWorkspace(workspaceId, true) == 0;
        boolean upgradeRecommended = subscription.getPlan() == Plan.FREE && upgradeAvailable;

        return new SubscriptionResponse(
                subscription.getPlan(),
                subscription.getStatus(),
                PlanLimits.isUnlimited(limits.maxChannelAccounts())
                        ? null
                        : limits.maxChannelAccounts(),
                PlanLimits.isUnlimited(limits.maxWorkflows()) ? null : limits.maxWorkflows(),
                PlanLimits.isUnlimited(limits.maxMembersPerWorkspace())
                        ? null
                        : limits.maxMembersPerWorkspace(),
                subscription.getCurrentPeriodEnd(),
                configuration.priceNgn().compareTo(BigDecimal.ZERO) > 0
                        ? configuration.priceNgn()
                        : null,
                configuration.billingInterval(),
                upgradeAvailable,
                upgradeRecommended,
                subscription.getDowngradeLockedChannels() > 0
                                || subscription.getDowngradeLockedWorkflows() > 0
                        ? subscription.getDowngradeLockedChannels()
                        : null,
                subscription.getDowngradeLockedWorkflows() > 0
                                || subscription.getDowngradeLockedChannels() > 0
                        ? subscription.getDowngradeLockedWorkflows()
                        : null);
    }

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Creates a FREE subscription row for a newly created workspace. Must be called inside the same
     * transaction as the workspace creation, so both rows are committed together.
     */
    @Transactional
    public void createFreeSubscription(Workspace workspace) {
        WorkspaceSubscription subscription = new WorkspaceSubscription();
        subscription.setWorkspace(workspace);
        subscription.setPlan(Plan.FREE);
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        subscriptionRepository.save(subscription);
    }

    /**
     * Activates a paid plan for the workspace. Called by the payment-provider webhook handler after
     * a successful charge.
     */
    @Transactional
    public void activate(
            UUID workspaceId,
            Plan plan,
            PaymentProvider provider,
            String customerCode,
            String subscriptionCode,
            Instant currentPeriodEnd) {
        WorkspaceSubscription subscription = findForUpdate(workspaceId);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPaymentProvider(provider);
        subscription.setPaymentCustomerCode(customerCode);
        subscription.setPaymentSubscriptionCode(subscriptionCode);
        subscription.setCurrentPeriodEnd(currentPeriodEnd);
        subscription.setDowngradeLockedChannels(0);
        subscription.setDowngradeLockedWorkflows(0);
        subscriptionRepository.save(subscription);

        log.info(
                "Plan activated: workspaceId={}, plan={}, provider={}, subscriptionCode={}",
                workspaceId,
                plan,
                provider,
                subscriptionCode);
    }

    /**
     * Marks the subscription as past-due (renewal payment failed). The workspace keeps its current
     * plan during the grace period; the downgrade scheduler handles expiry.
     */
    @Transactional
    public void markPastDue(UUID workspaceId) {
        WorkspaceSubscription subscription = findForUpdate(workspaceId);
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);

        log.info("Subscription marked past-due: workspaceId={}", workspaceId);
    }

    /**
     * Renews an existing subscription — updates {@code currentPeriodEnd} after a recurring charge
     * succeeds and stores the email token when provided.
     */
    @Transactional
    public void renew(UUID workspaceId, Instant newPeriodEnd, String emailToken) {
        WorkspaceSubscription subscription = findForUpdate(workspaceId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(newPeriodEnd);

        if (emailToken != null && !emailToken.isBlank()) {
            subscription.setPaymentSubscriptionToken(emailToken);
        }

        subscriptionRepository.save(subscription);

        log.info("Subscription renewed: workspaceId={}, periodEnd={}", workspaceId, newPeriodEnd);
    }

    /**
     * Schedules cancellation at the period end. Calls the billing provider to stop future charges,
     * then sets status to {@link SubscriptionStatus#CANCELLATION_SCHEDULED}.
     *
     * <p>Rejects with 503 if the subscription token is missing (the provider has not yet sent the
     * subscription-create webhook) or if no billing provider is configured.
     */
    @Transactional
    public void scheduleCancel(UUID workspaceId) {
        WorkspaceSubscription subscription = findForUpdate(workspaceId);

        if (subscription.getPaymentSubscriptionToken() == null
                || subscription.getPaymentSubscriptionToken().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to cancel right now. Please try again in a few moments or contact"
                            + " support.");
        }

        BillingProvider provider = findProvider(subscription.getPaymentProvider());

        if (provider == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment processing is not configured on this server.");
        }

        try {
            provider.cancel(
                    subscription.getPaymentSubscriptionCode(),
                    subscription.getPaymentSubscriptionToken());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Error cancelling subscription with billing provider: workspaceId={}: {}",
                    workspaceId,
                    e.getMessage());

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not reach the payment provider. Please try again.");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLATION_SCHEDULED);
        subscriptionRepository.save(subscription);

        log.info("Cancellation scheduled: workspaceId={}", workspaceId);
    }

    /**
     * Marks the subscription as cancellation-scheduled without calling the billing provider.
     *
     * <p>Called by the webhook handler when the provider sends {@code subscription.not_renew} — the
     * subscription is still active but will not renew, which may have been triggered either by the
     * user cancelling through our UI (idempotent) or directly from the provider dashboard.
     */
    @Transactional
    public void markCancellationScheduled(UUID workspaceId) {
        WorkspaceSubscription subscription = findForUpdate(workspaceId);
        subscription.setStatus(SubscriptionStatus.CANCELLATION_SCHEDULED);
        subscriptionRepository.save(subscription);

        log.info("Cancellation scheduled via provider event: workspaceId={}", workspaceId);
    }

    /**
     * Downgrades the workspace to FREE, disabling excess channel accounts and workflows (newest
     * first, up to the FREE plan limits). Sends an email and stores locked counts for the in-app
     * notice.
     *
     * <p>Called by the {@code subscription.disable} webhook handler and by the downgrade scheduler.
     */
    @Transactional
    public void downgrade(UUID workspaceId) {
        WorkspaceSubscription subscription = findForUpdate(workspaceId);
        PlanLimits freeLimits = planConfigurationService.getLimits(Plan.FREE);

        int lockedChannels = disableExcessChannels(workspaceId, freeLimits.maxChannelAccounts());
        int lockedWorkflows = disableExcessWorkflows(workspaceId, freeLimits.maxWorkflows());

        subscription.setPlan(Plan.FREE);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCurrentPeriodEnd(null);
        subscription.setDowngradeLockedChannels(lockedChannels);
        subscription.setDowngradeLockedWorkflows(lockedWorkflows);
        subscriptionRepository.save(subscription);

        notifyOwner(subscription.getWorkspace(), lockedChannels, lockedWorkflows);

        log.info(
                "Workspace downgraded to FREE: workspaceId={}, lockedChannels={},"
                        + " lockedWorkflows={}",
                workspaceId,
                lockedChannels,
                lockedWorkflows);
    }

    /**
     * Disables channel accounts and workflows over the FREE plan limits and records the locked
     * counts on the workspace's (already-FREE) subscription.
     *
     * <p>Guest workspaces are exempt from plan limits while the owner is anonymous (see {@link
     * #enforceLimit}), so a guest may accumulate more resources than FREE allows. Called once the
     * owner converts to a real account, so the workspace ends up in the same state a normal FREE
     * workspace would be in.
     */
    @Transactional
    public void lockExcessFreeResources(UUID workspaceId) {
        WorkspaceSubscription subscription = findForUpdate(workspaceId);
        PlanLimits freeLimits = planConfigurationService.getLimits(Plan.FREE);

        int lockedChannels = disableExcessChannels(workspaceId, freeLimits.maxChannelAccounts());
        int lockedWorkflows = disableExcessWorkflows(workspaceId, freeLimits.maxWorkflows());

        subscription.setDowngradeLockedChannels(lockedChannels);
        subscription.setDowngradeLockedWorkflows(lockedWorkflows);
        subscriptionRepository.save(subscription);

        log.info(
                "Locked excess resources after guest conversion: workspaceId={}, lockedChannels={},"
                        + " lockedWorkflows={}",
                workspaceId,
                lockedChannels,
                lockedWorkflows);
    }

    // ── Limit enforcement ─────────────────────────────────────────────────────

    /**
     * Throws {@link PlanLimitExceededException} (HTTP 402) if {@code count} is at or above the
     * limit for {@code limitType} on the workspace's current plan.
     *
     * <p>Limits are read live from Redis so changes take effect without a restart. Call this
     * <em>before</em> persisting the new resource.
     *
     * <p>Guest workspaces are exempt — their data is deleted within {@code
     * relayflow.guest.expiry-hours} regardless, so plan limits don't apply.
     */
    public void enforceLimit(UUID workspaceId, LimitType limitType, long count) {
        if (memberRepository.countByWorkspace(workspaceId, true) > 0) {
            return;
        }

        Plan plan = findPlan(workspaceId);
        PlanLimits limits = planConfigurationService.getLimits(plan);

        enforce(limits, plan, limitType, count);
    }

    /**
     * Checks the active-resource count before re-enabling a channel account or workflow. Uses the
     * enabled count (not total) so users can swap disabled ↔ enabled freely within the plan limit.
     *
     * <p>Guest workspaces are exempt — see {@link #enforceLimit}.
     */
    public void enforceLimitOnEnable(UUID workspaceId, LimitType limitType) {
        if (memberRepository.countByWorkspace(workspaceId, true) > 0) {
            return;
        }

        Plan plan = findPlan(workspaceId);
        PlanLimits limits = planConfigurationService.getLimits(plan);

        long count =
                switch (limitType) {
                    case CHANNEL_ACCOUNTS ->
                            channelAccountRepository.countBillable(
                                    workspaceId, ChannelAccountStatus.ACTIVE);
                    case WORKFLOWS -> workflowRepository.countEnabled(workspaceId);
                    default ->
                            throw new IllegalArgumentException(
                                    "enableLimit not applicable for: " + limitType);
                };

        enforce(limits, plan, limitType, count);
    }

    // ── Lookup by payment provider codes ──────────────────────────────────────

    /**
     * Finds the workspace whose subscription has the given provider subscription code. Returns
     * {@code null} if no match is found.
     */
    public WorkspaceSubscription findBySubscriptionCode(String subscriptionCode) {

        return subscriptionRepository.findByPaymentSubscriptionCode(subscriptionCode).orElse(null);
    }

    /**
     * Finds the workspace whose subscription has the given provider customer code. Returns {@code
     * null} if no match is found.
     *
     * <p>Used as a fallback in the {@code subscription.create} webhook handler when the initial
     * {@code charge.success} activated the workspace without a subscription code.
     */
    public WorkspaceSubscription findByCustomerCode(String customerCode) {

        return subscriptionRepository.findByPaymentCustomerCode(customerCode).orElse(null);
    }

    /**
     * Stores the provider subscription code on an already-active subscription.
     *
     * <p>Called by the {@code subscription.create} webhook handler when the workspace was activated
     * via {@code charge.success} before Paystack had assigned a subscription code.
     */
    @Transactional
    public void setSubscriptionCode(UUID workspaceId, String subscriptionCode) {
        WorkspaceSubscription subscription = findForUpdate(workspaceId);
        subscription.setPaymentSubscriptionCode(subscriptionCode);
        subscriptionRepository.save(subscription);

        log.info(
                "Subscription code stored: workspaceId={}, subscriptionCode={}",
                workspaceId,
                subscriptionCode);
    }

    /**
     * Best-effort: fetches the exact {@code nextPaymentDate} and {@code emailToken} from the
     * billing provider and updates the subscription.
     *
     * <p>Called right after {@link #activate} to replace the approximated period end with the value
     * Paystack has on record, and to store the email token needed for cancellation. This handles
     * the common case where {@code subscription.create} arrives before {@code charge.success} and
     * its data was therefore dropped.
     *
     * <p>Any error is logged as a warning and swallowed — the approximation set by {@link
     * #activate} is close enough that a failure here is not critical.
     */
    public void refreshPeriodEnd(UUID workspaceId) {
        WorkspaceSubscription subscription = find(workspaceId);

        if (subscription.getPaymentProvider() == null
                || subscription.getPaymentSubscriptionCode() == null) {
            return;
        }

        try {
            BillingProvider provider = findProvider(subscription.getPaymentProvider());
            BillingProvider.SubscriptionVerification verification =
                    provider.verify(subscription.getPaymentSubscriptionCode());

            if (verification.nextPaymentDate() != null) {
                renew(workspaceId, verification.nextPaymentDate(), verification.emailToken());

                log.info(
                        "Period end refreshed from provider: workspaceId={}, periodEnd={}",
                        workspaceId,
                        verification.nextPaymentDate());
            }
        } catch (Exception e) {
            log.warn(
                    "Could not refresh period end from billing provider for workspaceId={}: {}",
                    workspaceId,
                    e.getMessage());
        }
    }

    /**
     * Returns all subscriptions scheduled for cancellation whose period has ended, and all past-due
     * subscriptions whose grace period (period end + 7 days) has passed.
     *
     * <p>Used by the downgrade scheduler.
     */
    public List<WorkspaceSubscription> findDueForDowngrade(Instant now) {
        Instant graceCutoff = now.minusSeconds(7L * 24 * 3600);

        return subscriptionRepository.findDueForDowngrade(
                now,
                graceCutoff,
                SubscriptionStatus.CANCELLATION_SCHEDULED,
                SubscriptionStatus.PAST_DUE);
    }

    private void enforce(PlanLimits limits, Plan plan, LimitType limitType, long count) {
        int max =
                switch (limitType) {
                    case CHANNEL_ACCOUNTS -> limits.maxChannelAccounts();
                    case WORKFLOWS -> limits.maxWorkflows();
                    case MEMBERS_PER_WORKSPACE -> limits.maxMembersPerWorkspace();
                    default ->
                            throw new IllegalArgumentException("Unknown limit type: " + limitType);
                };

        if (PlanLimits.isUnlimited(max)) {
            return;
        }

        if (count >= max) {
            throw new PlanLimitExceededException(plan, limitType, max);
        }
    }

    private Plan findPlan(UUID workspaceId) {

        return find(workspaceId).getPlan();
    }

    /**
     * Returns the subscription for the workspace. If no row exists, returns a transient FREE
     * subscription so the system degrades gracefully.
     */
    private WorkspaceSubscription find(UUID workspaceId) {

        return subscriptionRepository
                .findByWorkspaceId(workspaceId)
                .orElseGet(
                        () -> {
                            WorkspaceSubscription fallback = new WorkspaceSubscription();
                            fallback.setPlan(Plan.FREE);
                            fallback.setStatus(SubscriptionStatus.ACTIVE);

                            return fallback;
                        });
    }

    /** Returns the billing provider matching {@code provider}, or {@code null} if not found. */
    private BillingProvider findProvider(PaymentProvider provider) {
        if (provider == null) {
            return null;
        }

        return billingProviders.stream()
                .filter(billingProvider -> billingProvider.provider() == provider)
                .findFirst()
                .orElse(null);
    }

    private WorkspaceSubscription findForUpdate(UUID workspaceId) {

        return subscriptionRepository
                .findByWorkspaceId(workspaceId)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No subscription row for workspace " + workspaceId));
    }

    private int disableExcessChannels(UUID workspaceId, int limit) {
        if (PlanLimits.isUnlimited(limit)) {
            return 0;
        }

        List<ChannelAccount> accounts =
                channelAccountRepository.findBillable(
                        workspaceId, Sort.by(Direction.DESC, "createdAt"));
        int excess = Math.max(0, accounts.size() - limit);

        accounts.stream()
                .limit(excess)
                .forEach(
                        account -> {
                            account.setStatus(ChannelAccountStatus.DISABLED);
                            channelAccountRepository.save(account);
                        });

        return excess;
    }

    private int disableExcessWorkflows(UUID workspaceId, int limit) {
        if (PlanLimits.isUnlimited(limit)) {
            return 0;
        }

        List<WorkflowDefinition> workflows = workflowRepository.findNewestFirst(workspaceId);
        int excess = Math.max(0, workflows.size() - limit);

        workflows.stream()
                .limit(excess)
                .forEach(
                        workflow -> {
                            workflow.setEnabled(false);
                            workflowRepository.save(workflow);
                        });

        return excess;
    }

    private void notifyOwner(Workspace workspace, int lockedChannels, int lockedWorkflows) {
        memberRepository.findByWorkspace(workspace.getId()).stream()
                .filter(member -> member.getRole() == WorkspaceRole.OWNER)
                .findFirst()
                .flatMap(owner -> userRepository.findById(owner.getUserId()))
                .ifPresent(
                        (User owner) ->
                                emailService.sendDowngradeNotice(
                                        owner.getEmail(),
                                        workspace.getName(),
                                        lockedChannels,
                                        lockedWorkflows));
    }
}
