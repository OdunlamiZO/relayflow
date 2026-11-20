package io.odunlamizo.relayflow.knowledge;

// Docs: docs/codebase-components.md#knowledgesourcefetcher

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSource;
import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSourceHeader;
import io.odunlamizo.relayflow.workspace.SecretService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Fetches a knowledge source's JSON, keeping successful responses for its cache time. */
@Component
public class KnowledgeSourceFetcher {

    static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static final Pattern SECRET_PLACEHOLDER =
            Pattern.compile("\\{\\{secrets\\.([A-Za-z0-9_]+)}}");

    private final ObjectMapper objectMapper;

    private final SecretService secretService;

    private final HttpClient httpClient;

    private final Map<UUID, CachedResult> cache = new ConcurrentHashMap<>();

    public KnowledgeSourceFetcher(ObjectMapper objectMapper, SecretService secretService) {
        this.objectMapper = objectMapper;
        this.secretService = secretService;
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
    }

    public CompletableFuture<FetchResult> fetch(KnowledgeSource source) {
        CachedResult cached = cache.get(source.getId());
        Instant now = Instant.now();

        if (cached != null
                && cached.sourceUpdatedAt().equals(source.getUpdatedAt())
                && cached.expiresAt().isAfter(now)) {
            return CompletableFuture.completedFuture(cached.result());
        }

        return fetchUncached(source.getWorkspaceId(), source.getUrl(), source.getHeaders())
                .thenApply(
                        result -> {
                            if (!result.failed() && source.getCacheSeconds() > 0) {
                                cache.put(
                                        source.getId(),
                                        new CachedResult(
                                                source.getUpdatedAt(),
                                                now.plusSeconds(source.getCacheSeconds()),
                                                result));
                            }

                            return result;
                        });
    }

    public CompletableFuture<FetchResult> fetchUncached(
            UUID workspaceId, String url, List<KnowledgeSourceHeader> headers) {
        HttpRequest request;

        try {
            HttpRequest.Builder builder =
                    HttpRequest.newBuilder()
                            .uri(URI.create(fillSecrets(workspaceId, url)))
                            .timeout(TIMEOUT)
                            .header("Accept", "application/json")
                            .GET();

            for (KnowledgeSourceHeader header :
                    headers != null ? headers : List.<KnowledgeSourceHeader>of()) {
                if (header.key() != null && !header.key().isBlank()) {
                    builder.setHeader(
                            header.key().trim(),
                            fillSecrets(workspaceId, header.value() != null ? header.value() : ""));
                }
            }

            request = builder.build();
        } catch (UnknownSecretException exception) {
            return CompletableFuture.completedFuture(
                    FetchResult.failure(null, null, "Unknown secret: " + exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return CompletableFuture.completedFuture(
                    FetchResult.failure(null, null, "The URL or a header isn't valid"));
        }

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::toResult)
                .exceptionally(this::toFailure);
    }

    void evict(UUID sourceId) {
        cache.remove(sourceId);
    }

    private FetchResult toResult(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String body = response.body();

        if (statusCode < 200 || statusCode >= 300) {
            return FetchResult.failure(statusCode, body, "HTTP " + statusCode);
        }

        try {
            JsonNode json = objectMapper.readTree(body != null ? body : "");

            if (json == null || json.isMissingNode()) {
                return FetchResult.failure(statusCode, body, "The response is empty");
            }

            return new FetchResult(statusCode, body, json, null);
        } catch (Exception exception) {
            return FetchResult.failure(statusCode, body, "The response isn't JSON");
        }
    }

    private FetchResult toFailure(Throwable throwable) {
        Throwable cause =
                throwable instanceof CompletionException ? throwable.getCause() : throwable;

        if (cause instanceof HttpTimeoutException) {
            return FetchResult.failure(
                    null, null, "Timed out after " + TIMEOUT.toSeconds() + " seconds");
        }

        String message = cause != null ? cause.getMessage() : null;

        return FetchResult.failure(
                null,
                null,
                message != null && !message.isBlank()
                        ? "Request failed: " + message
                        : "Request failed: "
                                + (cause != null ? cause.getClass().getSimpleName() : "unknown"));
    }

    private String fillSecrets(UUID workspaceId, String text) {
        if (text == null || !text.contains("{{secrets.")) {
            return text;
        }

        Matcher matcher = SECRET_PLACEHOLDER.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String name = matcher.group(1);
            String value =
                    secretService
                            .resolveDecrypted(workspaceId, name)
                            .orElseThrow(() -> new UnknownSecretException(name));

            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    /** {@code json} is set only when the fetch succeeded. */
    public record FetchResult(Integer statusCode, String body, JsonNode json, String error) {

        static FetchResult failure(Integer statusCode, String body, String error) {
            return new FetchResult(statusCode, body, null, error);
        }

        public boolean failed() {
            return error != null;
        }
    }

    private record CachedResult(Instant sourceUpdatedAt, Instant expiresAt, FetchResult result) {}

    private static class UnknownSecretException extends RuntimeException {

        UnknownSecretException(String name) {
            super(name);
        }
    }
}
