package com.relayflow.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfiguration {

    @Bean
    RestTemplate restTemplate(
            @Value("${relayflow.http.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${relayflow.http.read-timeout-ms}") int readTimeoutMs) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        return new RestTemplate(factory);
    }
}
