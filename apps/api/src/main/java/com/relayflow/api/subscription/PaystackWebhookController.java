package com.relayflow.api.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.subscription.domain.PaymentProvider;
import com.relayflow.api.subscription.domain.Plan;
import com.relayflow.api.subscription.domain.WorkspaceSubscription;
import com.relayflow.api.subscription.paystack.PaystackChargeData;
import com.relayflow.api.subscription.paystack.PaystackSubscriptionData;
import io.github.odunlamizo.paystack.Paystack;
import io.github.odunlamizo.paystack.PaystackException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives Paystack webhook events and updates workspace subscription state.
 *
 * <p>Handled events:
 *
 * <ul>
 *   <li>{@code charge.success} — activates PRO for the workspace encoded in metadata.
 *   <li>{@code subscription.create} — sets {@code currentPeriodEnd} from {@code next_payment_date}.
 *   <li>{@code subscription.not_renew} — marks cancellation scheduled; subscription stays active
 *       until the current period ends. Fired when the subscription is disabled via the Paystack
 *       dashboard or our in-app cancel flow.
 *   <li>{@code subscription.disable} — subscription has been fully disabled; downgrades to FREE.
 *   <li>{@code subscription.expiring_cards} — informational; logged and ignored.
 *   <li>{@code invoice.update} — marks the subscription past-due on payment failure.
 * </ul>
 *
 * <p>Auto-renewal is handled entirely by Paystack — recurring charges arrive as {@code
 * charge.success} events and are processed the same way as the initial charge.
 */
@RestController
@RequestMapping("/paystack/webhook")
public class PaystackWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaystackWebhookController.class);

    /**
     * Holds subscription.create payloads that arrived before charge.success committed the workspace
     * activation. Keyed by Paystack customer code. charge.success drains this after activating so
     * the subscription code and email token are never lost to the race.
     */
    private final ConcurrentHashMap<String, PaystackSubscriptionData> pendingSubscriptionCreate =
            new ConcurrentHashMap<>();

    private final Optional<Paystack> paystack;

    private final SubscriptionService subscriptionService;

    private final PlanConfigurationService planConfigurationService;

    private final ObjectMapper objectMapper;

    public PaystackWebhookController(
            Optional<Paystack> paystack,
            SubscriptionService subscriptionService,
            PlanConfigurationService planConfigurationService,
            ObjectMapper objectMapper) {
        this.paystack = paystack;
        this.subscriptionService = subscriptionService;
        this.planConfigurationService = planConfigurationService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Void> webhook(
            @RequestBody String payload, @RequestHeader("x-paystack-signature") String signature) {

        if (paystack.isEmpty()) {
            log.warn("Paystack webhook received but Paystack is not configured — ignoring");

            return ResponseEntity.ok().build();
        }

        try {
            paystack.get()
                    .processWebhook(
                            payload,
                            signature,
                            body -> {
                                try {
                                    handleEvent(body);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });

        } catch (PaystackException e) {
            log.warn("Paystack webhook signature validation failed");

            return ResponseEntity.status(401).build();

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Paystack webhook crypto error: {}", e.getMessage());

            return ResponseEntity.internalServerError().build();

        } catch (Exception e) {
            log.error("Paystack webhook processing error: {}", e.getMessage(), e);

            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }

    // ── Event dispatch ────────────────────────────────────────────────────────

    private void handleEvent(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        String event = root.path("event").asText();

        log.info("Paystack webhook: event={}", event);

        switch (event) {
            case "charge.success" -> handleChargeSuccess(root.path("data"));
            case "subscription.create" -> handleSubscriptionCreate(root.path("data"));
            case "subscription.not_renew" -> handleSubscriptionNotRenew(root.path("data"));
            case "subscription.disable" -> handleSubscriptionDisable(root.path("data"));
            case "invoice.update" -> handleInvoiceUpdate(root.path("data"));
            case "subscription.expiring_cards" ->
                    log.info("Paystack webhook: expiring cards notification received — no action");
            default -> log.debug("Paystack webhook: unhandled event={}", event);
        }
    }

    // ── charge.success ────────────────────────────────────────────────────────

    private void handleChargeSuccess(JsonNode data) throws Exception {
        PaystackChargeData charge = objectMapper.treeToValue(data, PaystackChargeData.class);

        UUID workspaceId = extractWorkspaceId(charge.metadata());

        if (workspaceId != null) {
            // Initial charge — workspace_id is always present in metadata on the first payment.
            // subscription_code may be absent: Paystack creates the subscription asynchronously
            // and delivers it via a separate subscription.create event. We activate now with
            // whatever code is available (possibly null); subscription.create will fill it in.
            String paystackPlanCode = charge.plan() != null ? charge.plan().planCode() : null;
            Plan plan = resolveTargetPlan(paystackPlanCode, charge.metadata());
            String customerCode =
                    charge.customer() != null ? charge.customer().customerCode() : null;

            subscriptionService.activate(
                    workspaceId,
                    plan,
                    PaymentProvider.PAYSTACK,
                    customerCode,
                    charge.subscriptionCode(),
                    // Approximation — refreshPeriodEnd replaces it with the exact Paystack date.
                    Instant.now().plusSeconds(periodSeconds(plan)));

            // If subscription.create arrived before this commit, apply its data now.
            PaystackSubscriptionData pending =
                    customerCode != null ? pendingSubscriptionCreate.remove(customerCode) : null;

            if (pending != null && pending.subscriptionCode() != null) {
                log.debug(
                        "charge.success: applying cached subscription.create for customerCode={},"
                                + " subscriptionCode={}",
                        customerCode,
                        pending.subscriptionCode());

                subscriptionService.setSubscriptionCode(workspaceId, pending.subscriptionCode());

                if (pending.nextPaymentDate() != null) {
                    subscriptionService.renew(
                            workspaceId,
                            Instant.parse(pending.nextPaymentDate()),
                            pending.emailToken());
                }
            } else if (charge.subscriptionCode() != null && !charge.subscriptionCode().isBlank()) {
                // subscription_code already in the charge — fetch exact period end from Paystack.
                subscriptionService.refreshPeriodEnd(workspaceId);
            }

            return;
        }

        // No workspace_id in metadata → recurring renewal charge.
        // Must have subscription_code to identify which workspace to renew.
        if (charge.subscriptionCode() == null || charge.subscriptionCode().isBlank()) {
            log.debug(
                    "charge.success: no workspace_id and no subscription_code — ignoring, ref={}",
                    charge.reference());

            return;
        }

        WorkspaceSubscription subscription =
                subscriptionService.findBySubscriptionCode(charge.subscriptionCode());

        if (subscription == null) {
            log.warn(
                    "charge.success: no workspace found for subscriptionCode={}",
                    charge.subscriptionCode());

            return;
        }

        // Recurring renewal — update the period end.
        subscriptionService.renew(
                subscription.getWorkspace().getId(),
                Instant.now().plusSeconds(periodSeconds(subscription.getPlan())),
                null);
    }

    // ── subscription.create ───────────────────────────────────────────────────

    private void handleSubscriptionCreate(JsonNode data) throws Exception {
        PaystackSubscriptionData subscriptionData =
                objectMapper.treeToValue(data, PaystackSubscriptionData.class);

        if (subscriptionData.subscriptionCode() == null
                || subscriptionData.nextPaymentDate() == null) {
            return;
        }

        WorkspaceSubscription subscription =
                subscriptionService.findBySubscriptionCode(subscriptionData.subscriptionCode());

        if (subscription == null) {
            // charge.success may have activated the workspace without a subscription_code
            // (Paystack assigns it asynchronously). Fall back to a customer-code lookup so
            // we can store the code and exact period end on the already-active subscription.
            String customerCode =
                    subscriptionData.customer() != null
                            ? subscriptionData.customer().customerCode()
                            : null;

            if (customerCode != null) {
                subscription = subscriptionService.findByCustomerCode(customerCode);
            }

            if (subscription == null) {
                // charge.success may not have committed yet — cache the data so charge.success
                // can apply it after activation.
                if (customerCode != null) {
                    pendingSubscriptionCreate.put(customerCode, subscriptionData);
                }

                log.debug(
                        "subscription.create: no workspace for subscriptionCode={} or"
                                + " customerCode={} — cached for charge.success",
                        subscriptionData.subscriptionCode(),
                        customerCode);

                return;
            }

            // Workspace was activated without a code — store it now.
            subscriptionService.setSubscriptionCode(
                    subscription.getWorkspace().getId(), subscriptionData.subscriptionCode());
        }

        Instant nextPayment = Instant.parse(subscriptionData.nextPaymentDate());
        subscriptionService.renew(
                subscription.getWorkspace().getId(), nextPayment, subscriptionData.emailToken());
    }

    // ── subscription.not_renew ────────────────────────────────────────────────

    private void handleSubscriptionNotRenew(JsonNode data) throws Exception {
        PaystackSubscriptionData subscriptionData =
                objectMapper.treeToValue(data, PaystackSubscriptionData.class);

        if (subscriptionData.subscriptionCode() == null) {
            return;
        }

        WorkspaceSubscription subscription =
                subscriptionService.findBySubscriptionCode(subscriptionData.subscriptionCode());

        if (subscription == null) {
            log.debug(
                    "subscription.not_renew: no workspace for subscriptionCode={}",
                    subscriptionData.subscriptionCode());

            return;
        }

        subscriptionService.markCancellationScheduled(subscription.getWorkspace().getId());
    }

    // ── subscription.disable ──────────────────────────────────────────────────

    private void handleSubscriptionDisable(JsonNode data) throws Exception {
        PaystackSubscriptionData subscriptionData =
                objectMapper.treeToValue(data, PaystackSubscriptionData.class);

        if (subscriptionData.subscriptionCode() == null) {
            return;
        }

        WorkspaceSubscription subscription =
                subscriptionService.findBySubscriptionCode(subscriptionData.subscriptionCode());

        if (subscription == null) {
            log.debug(
                    "subscription.disable: no workspace for subscriptionCode={}",
                    subscriptionData.subscriptionCode());

            return;
        }

        subscriptionService.downgrade(subscription.getWorkspace().getId());
    }

    // ── invoice.update ────────────────────────────────────────────────────────

    private void handleInvoiceUpdate(JsonNode data) throws Exception {
        String subscriptionCode = data.path("subscription").path("subscription_code").asText(null);
        String paid = data.path("paid").asText(null);

        if (subscriptionCode == null) {
            return;
        }

        // "false" or "0" means the invoice was not paid — mark past-due.
        if ("false".equals(paid) || "0".equals(paid)) {
            WorkspaceSubscription ws = subscriptionService.findBySubscriptionCode(subscriptionCode);

            if (ws != null) {
                subscriptionService.markPastDue(ws.getWorkspace().getId());
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long periodSeconds(Plan plan) {

        return planConfigurationService.periodSeconds(plan);
    }

    private UUID extractWorkspaceId(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }

        Object raw = metadata.get("workspace_id");

        if (raw == null) {
            return null;
        }

        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid workspace_id in Paystack metadata: {}", raw);

            return null;
        }
    }

    /**
     * Resolves which {@link Plan} corresponds to the Paystack plan code that triggered the charge.
     * Falls back to PRO if the code is not recognized (safe default for a two-tier model).
     */
    private Plan resolveTargetPlan(String paystackPlanCode, Map<String, Object> metadata) {
        // Prefer the plan value encoded directly in metadata.
        if (metadata != null && metadata.get("plan") != null) {
            try {
                return Plan.valueOf(metadata.get("plan").toString().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }

        // Match against every known plan's configured Paystack plan code.
        for (Plan plan : Plan.values()) {
            if (plan == Plan.FREE) {
                continue;
            }

            String code = planConfigurationService.getPaystackPlanCode(plan);

            if (paystackPlanCode != null && paystackPlanCode.equals(code)) {
                return plan;
            }
        }

        // Last resort: return the first purchasable paid plan.
        for (Plan plan : Plan.values()) {
            if (plan != Plan.FREE && planConfigurationService.isPurchasable(plan)) {
                log.warn(
                        "Could not resolve plan for paystackPlanCode={} — falling back to {}",
                        paystackPlanCode,
                        plan);

                return plan;
            }
        }

        throw new IllegalStateException(
                "Could not resolve plan for paystackPlanCode="
                        + paystackPlanCode
                        + " and no purchasable fallback plan found.");
    }
}
