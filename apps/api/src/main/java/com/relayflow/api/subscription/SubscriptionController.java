package com.relayflow.api.subscription;

import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.messaging.WorkspaceAuthorizationService;
import com.relayflow.api.subscription.dto.CheckoutResponse;
import com.relayflow.api.subscription.dto.StartCheckoutRequest;
import com.relayflow.api.subscription.dto.SubscriptionResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workspaces/{workspaceId}/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    private final SubscriptionCheckoutService checkoutService;

    private final WorkspaceAuthorizationService authorizationService;

    private final SecurityUtils securityUtils;

    public SubscriptionController(
            SubscriptionService subscriptionService,
            SubscriptionCheckoutService checkoutService,
            WorkspaceAuthorizationService authorizationService,
            SecurityUtils securityUtils) {
        this.subscriptionService = subscriptionService;
        this.checkoutService = checkoutService;
        this.authorizationService = authorizationService;
        this.securityUtils = securityUtils;
    }

    /** Returns the current plan, status, per-plan limits, and pricing for the workspace. */
    @GetMapping
    public SubscriptionResponse getSubscription(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertMember(workspaceId, authentication);

        return subscriptionService.getResponse(workspaceId);
    }

    /**
     * Initializes a Paystack checkout session and returns the authorization URL. The frontend
     * should redirect the user to this URL to complete payment.
     */
    @PostMapping("/checkout")
    public CheckoutResponse startCheckout(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody StartCheckoutRequest request,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        UUID userId = securityUtils.resolveUserId(authentication);
        String url = checkoutService.initializeCheckout(workspaceId, request.plan(), userId);

        return new CheckoutResponse(url);
    }

    /**
     * Cancels the workspace subscription. Calls Paystack to stop future charges, then schedules the
     * downgrade for the end of the current billing period.
     *
     * <p>Returns 503 if the Paystack email token has not yet been received (webhook latency) or if
     * Paystack is not configured.
     */
    @DeleteMapping
    public ResponseEntity<Void> cancel(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);
        subscriptionService.scheduleCancel(workspaceId);

        return ResponseEntity.noContent().build();
    }
}
