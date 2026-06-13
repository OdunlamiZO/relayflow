package com.relayflow.api.authentication;

import com.relayflow.api.messaging.ApiKeyService;
import com.relayflow.api.messaging.domain.WorkspaceApiKey;
import com.relayflow.api.messaging.repository.WorkspaceApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the {@code X-Api-Key} header, verifies the key against the DB, and sets an {@link
 * ApiKeyAuthentication} in the SecurityContext if valid.
 *
 * <p>{@code lastUsedAt} is updated at most once per hour per key to avoid a DB write on every
 * single request.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Api-Key";
    private static final Duration LAST_USED_UPDATE_INTERVAL = Duration.ofHours(1);

    private final WorkspaceApiKeyRepository apiKeyRepository;

    public ApiKeyAuthenticationFilter(WorkspaceApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {
        String rawKey = request.getHeader(HEADER);

        if (rawKey != null && !rawKey.isBlank()) {
            String hash = ApiKeyService.sha256(rawKey.trim());

            apiKeyRepository
                    .findByKeyHash(hash)
                    .filter(key -> !key.isRevoked())
                    .filter(key -> !key.isExpired())
                    .ifPresent(
                            key -> {
                                maybeUpdateLastUsed(key);
                                SecurityContextHolder.getContext()
                                        .setAuthentication(
                                                new ApiKeyAuthentication(key.getWorkspaceId()));
                            });
        }

        chain.doFilter(request, response);
    }

    private void maybeUpdateLastUsed(WorkspaceApiKey key) {
        Instant now = Instant.now();
        Instant lastUsed = key.getLastUsedAt();

        if (lastUsed == null
                || Duration.between(lastUsed, now).compareTo(LAST_USED_UPDATE_INTERVAL) > 0) {
            key.setLastUsedAt(now);
            apiKeyRepository.save(key);
        }
    }
}
