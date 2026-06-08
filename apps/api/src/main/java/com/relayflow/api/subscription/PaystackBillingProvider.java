package com.relayflow.api.subscription;

import com.relayflow.api.subscription.domain.PaymentProvider;
import io.github.odunlamizo.paystack.Paystack;
import io.github.odunlamizo.paystack.model.DisableSubscriptionRequest;
import io.github.odunlamizo.paystack.model.SubscriptionData;
import java.io.IOException;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Paystack implementation of {@link BillingProvider}. Active only when the Paystack bean is
 * configured.
 */
@Component
@ConditionalOnBean(Paystack.class)
public class PaystackBillingProvider implements BillingProvider {

    private final Paystack paystack;

    public PaystackBillingProvider(Paystack paystack) {
        this.paystack = paystack;
    }

    @Override
    public PaymentProvider provider() {

        return PaymentProvider.PAYSTACK;
    }

    @Override
    public SubscriptionVerification verify(String subscriptionCode) throws IOException {
        var response = paystack.fetchSubscription(subscriptionCode);

        if (!response.isStatus() || response.getData() == null) {
            // Cannot confirm status — treat as inactive (conservative; caller will proceed with
            // downgrade).
            return new SubscriptionVerification(false, null, null);
        }

        SubscriptionData data = response.getData();
        boolean active = "active".equalsIgnoreCase(data.getStatus());
        Instant nextPaymentDate =
                data.getNextPaymentDate() != null ? Instant.parse(data.getNextPaymentDate()) : null;

        return new SubscriptionVerification(active, nextPaymentDate, data.getEmailToken());
    }

    @Override
    public void cancel(String subscriptionCode, String emailToken) throws IOException {
        DisableSubscriptionRequest request =
                DisableSubscriptionRequest.builder()
                        .code(subscriptionCode)
                        .token(emailToken)
                        .build();

        var response = paystack.disableSubscription(request);

        if (!response.isStatus()) {
            throw new IOException(
                    "Billing provider rejected cancellation: " + response.getMessage());
        }
    }
}
