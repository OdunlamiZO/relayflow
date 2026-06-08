package com.relayflow.api.subscription;

import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.messaging.domain.WorkspaceMember;
import com.relayflow.api.messaging.domain.WorkspaceRole;
import com.relayflow.api.messaging.repository.WorkspaceMemberRepository;
import com.relayflow.api.subscription.domain.PaymentProvider;
import com.relayflow.api.subscription.domain.Plan;
import com.relayflow.api.subscription.domain.PlanConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Orchestrates plan upgrade checkout.
 *
 * <p>Reads the active {@link PaymentProvider} for the requested plan from Redis via {@link
 * PlanConfigurationService#getProvider}, then delegates to the matching {@link CheckoutProvider}
 * implementation. Adding a new payment provider requires only a new {@link CheckoutProvider}
 * {@code @Component} — no changes here.
 */
@Service
public class SubscriptionCheckoutService {

    private final Map<PaymentProvider, CheckoutProvider> providers;

    private final PlanConfigurationService planConfigurationService;

    private final WorkspaceMemberRepository memberRepository;

    private final UserRepository userRepository;

    public SubscriptionCheckoutService(
            List<CheckoutProvider> checkoutProviders,
            PlanConfigurationService planConfigurationService,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository) {
        this.providers =
                checkoutProviders.stream()
                        .collect(Collectors.toMap(CheckoutProvider::provider, Function.identity()));
        this.planConfigurationService = planConfigurationService;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    /**
     * Initializes a checkout session for upgrading the workspace to {@code plan}.
     *
     * @param workspaceId workspace to upgrade
     * @param plan target plan (must be a paid plan)
     * @param requesterId user requesting the upgrade (must be the workspace owner)
     * @return authorization URL to redirect the user to
     */
    public String initializeCheckout(UUID workspaceId, Plan plan, UUID requesterId) {
        if (plan == Plan.FREE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot check out for the FREE plan.");
        }

        PaymentProvider providerType = planConfigurationService.getProvider(plan);
        CheckoutProvider provider = providers.get(providerType);

        if (provider == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No checkout provider available for the " + plan.name() + " plan.");
        }

        PlanConfiguration configuration = planConfigurationService.getConfiguration(plan);
        String ownerEmail = resolveOwnerEmail(workspaceId, requesterId);

        return provider.initializeCheckout(workspaceId, plan, ownerEmail, configuration);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private String resolveOwnerEmail(UUID workspaceId, UUID requesterId) {
        WorkspaceMember requesterMembership =
                memberRepository
                        .findByWorkspaceAndUser(workspaceId, requesterId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.FORBIDDEN,
                                                "You are not a member of this workspace."));

        if (requesterMembership.getRole() != WorkspaceRole.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the workspace owner can manage billing.");
        }

        return userRepository
                .findById(requesterId)
                .map(User::getEmail)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
