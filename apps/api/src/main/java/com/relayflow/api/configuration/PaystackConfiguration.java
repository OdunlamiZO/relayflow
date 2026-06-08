package com.relayflow.api.configuration;

import io.github.odunlamizo.paystack.Paystack;
import io.github.odunlamizo.paystack.okhttp.PaystackOkHttp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaystackConfiguration {

    /**
     * Creates the Paystack client bean. Only registered when {@code paystack.secret-key} is
     * non-empty so the app can start without a key in local development (subscription features will
     * simply be unavailable).
     */
    @Bean
    @ConditionalOnProperty(name = "paystack.secret-key")
    public Paystack paystack(@Value("${paystack.secret-key}") String secretKey) {

        return new PaystackOkHttp(secretKey);
    }
}
