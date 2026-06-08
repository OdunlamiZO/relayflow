package com.relayflow.api.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.subscription.domain.PaymentProvider;
import com.relayflow.api.subscription.domain.Plan;
import com.relayflow.api.subscription.domain.PlanConfiguration;
import io.github.odunlamizo.paystack.Paystack;
import io.github.odunlamizo.paystack.model.InitializeTransactionRequest;
import io.github.odunlamizo.paystack.model.InitializeTransactionResponse;
import io.github.odunlamizo.paystack.model.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * {@link CheckoutProvider} implementation for Paystack.
 *
 * <p>Reads the Paystack plan code from Redis via {@link
 * PlanConfigurationService#getPaystackPlanCode}. Inactive (throws 503) when the {@code Paystack}
 * bean is absent, i.e., when {@code paystack.secret-key} is not configured.
 */
@Component
public class PaystackCheckoutProvider implements CheckoutProvider {

    private static final Logger log = LoggerFactory.getLogger(PaystackCheckoutProvider.class);

    private final Optional<Paystack> paystack;

    private final PlanConfigurationService planConfigurationService;

    private final ObjectMapper objectMapper;

    private final String webBaseUrl;

    public PaystackCheckoutProvider(
            Optional<Paystack> paystack,
            PlanConfigurationService planConfigurationService,
            ObjectMapper objectMapper,
            @Value("${relayflow.web.base-url}") String webBaseUrl) {
        this.paystack = paystack;
        this.planConfigurationService = planConfigurationService;
        this.objectMapper = objectMapper;
        this.webBaseUrl = webBaseUrl;
    }

    @Override
    public PaymentProvider provider() {

        return PaymentProvider.PAYSTACK;
    }

    @Override
    public String initializeCheckout(
            UUID workspaceId, Plan plan, String ownerEmail, PlanConfiguration configuration) {
        if (paystack.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment processing is not configured on this server.");
        }

        String planCode = planConfigurationService.getPaystackPlanCode(plan);

        if (planCode == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The "
                            + plan.name()
                            + " plan is not yet available for purchase. Check back soon.");
        }

        long amountKobo = configuration.priceNgn().multiply(BigDecimal.valueOf(100)).longValue();
        String metadata = buildMetadata(workspaceId, plan);
        String callbackUrl = webBaseUrl + "/settings?tab=billing&workspace=" + workspaceId;

        try {
            InitializeTransactionRequest request =
                    InitializeTransactionRequest.builder()
                            .email(ownerEmail)
                            .amount(String.valueOf(amountKobo))
                            .plan(planCode)
                            .channels(List.of("card"))
                            .metadata(metadata)
                            .callbackUrl(callbackUrl)
                            .build();

            Response<InitializeTransactionResponse> response =
                    paystack.get().initializeTransaction(request);

            if (!response.isStatus() || response.getData() == null) {
                log.warn(
                        "Paystack checkout init failed: workspaceId={}, plan={}, msg={}",
                        workspaceId,
                        plan,
                        response.getMessage());

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Could not initialize payment. Please try again.");
            }

            log.info(
                    "Paystack checkout initialized: workspaceId={}, plan={}, ref={}",
                    workspaceId,
                    plan,
                    response.getData().getReference());

            return response.getData().getAuthorizationUrl();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Paystack checkout error: workspaceId={}, plan={}: {}",
                    workspaceId,
                    plan,
                    e.getMessage());

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not reach the payment provider. Please try again.");
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private String buildMetadata(UUID workspaceId, Plan plan) {

        try {
            return objectMapper.writeValueAsString(
                    Map.of("workspace_id", workspaceId.toString(), "plan", plan.name()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize checkout metadata", e);
        }
    }
}
