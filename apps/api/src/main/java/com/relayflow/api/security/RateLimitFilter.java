package com.relayflow.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.messaging.ApiKeyService;
import com.relayflow.api.messaging.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed-window rate limiting for abuse-prone endpoints — auth (login, signup, email verification),
 * the public API, and inbound channel webhooks — keyed by client IP or API key and backed by Redis
 * so limits are shared across instances.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String API_KEY_HEADER = "X-Api-Key";

    private final StringRedisTemplate redis;

    private final ObjectMapper objectMapper;

    private final boolean enabled;

    private final List<RateLimitRule> rules;

    public RateLimitFilter(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            boolean enabled,
            int loginLimit,
            int loginWindowSeconds,
            int signupLimit,
            int signupWindowSeconds,
            int publicApiLimit,
            int publicApiWindowSeconds,
            int webhookLimit,
            int webhookWindowSeconds) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.rules =
                List.of(
                        new RateLimitRule(
                                "login",
                                "/auth/login",
                                loginLimit,
                                loginWindowSeconds,
                                KeySource.IP_ADDRESS),
                        new RateLimitRule(
                                "login",
                                "/auth/login/2fa",
                                loginLimit,
                                loginWindowSeconds,
                                KeySource.IP_ADDRESS),
                        new RateLimitRule(
                                "login",
                                "/auth/reset-password/*",
                                loginLimit,
                                loginWindowSeconds,
                                KeySource.IP_ADDRESS),
                        new RateLimitRule(
                                "signup",
                                "/auth/signup",
                                signupLimit,
                                signupWindowSeconds,
                                KeySource.IP_ADDRESS),
                        new RateLimitRule(
                                "signup",
                                "/auth/bootstrap",
                                signupLimit,
                                signupWindowSeconds,
                                KeySource.IP_ADDRESS),
                        new RateLimitRule(
                                "public-api",
                                "/public/v1/**",
                                publicApiLimit,
                                publicApiWindowSeconds,
                                KeySource.API_KEY),
                        new RateLimitRule(
                                "webhook",
                                "/telegram/webhook/**",
                                webhookLimit,
                                webhookWindowSeconds,
                                KeySource.IP_ADDRESS),
                        new RateLimitRule(
                                "webhook",
                                "/whatsapp/webhook/**",
                                webhookLimit,
                                webhookWindowSeconds,
                                KeySource.IP_ADDRESS));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);

            return;
        }

        String path = request.getRequestURI();

        for (RateLimitRule rule : rules) {
            if (PATH_MATCHER.match(rule.path(), path) && !allow(rule, request)) {
                rejectRequest(response, rule);

                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean allow(RateLimitRule rule, HttpServletRequest request) {
        long window = System.currentTimeMillis() / (rule.windowSeconds() * 1000L);
        String key = "ratelimit:" + rule.bucket() + ":" + clientKey(rule, request) + ":" + window;

        Long count = redis.opsForValue().increment(key);

        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofSeconds(rule.windowSeconds()));
        }

        return count == null || count <= rule.limit();
    }

    private String clientKey(RateLimitRule rule, HttpServletRequest request) {
        if (rule.keySource() == KeySource.API_KEY) {
            String apiKey = request.getHeader(API_KEY_HEADER);

            if (apiKey != null && !apiKey.isBlank()) {
                return ApiKeyService.sha256(apiKey.trim());
            }
        }

        return clientIp(request);
    }

    private void rejectRequest(HttpServletResponse response, RateLimitRule rule)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(rule.windowSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse("Too many requests, please try again later.", Instant.now()));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private enum KeySource {
        IP_ADDRESS,
        API_KEY
    }

    private record RateLimitRule(
            String bucket, String path, int limit, int windowSeconds, KeySource keySource) {}
}
