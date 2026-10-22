package com.relayflow.api.configuration;

import com.relayflow.api.logging.RequestIdFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class LoggingConfiguration {

    private static final List<String> LOG_FORMATS =
            List.of("text", "json", "logfmt", "ecs", "gelf");

    public LoggingConfiguration(@Value("${LOG_FORMAT:text}") String logFormat) {
        if (!LOG_FORMATS.contains(logFormat)) {
            throw new IllegalStateException(
                    "LOG_FORMAT must be one of " + LOG_FORMATS + ", but was '" + logFormat + "'");
        }
    }

    @Bean
    FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration() {
        FilterRegistrationBean<RequestIdFilter> registration =
                new FilterRegistrationBean<>(new RequestIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }
}
