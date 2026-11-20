package io.odunlamizo.relayflow.knowledge;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.odunlamizo.relayflow.knowledge.KnowledgeSourceFetcher.FetchResult;
import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSource;
import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSourceHeader;
import io.odunlamizo.relayflow.workspace.SecretService;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeSourceFetcherTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    @Mock private SecretService secretService;

    private HttpServer server;

    private final AtomicInteger requests = new AtomicInteger();

    private final AtomicReference<String> authorization = new AtomicReference<>();

    private final AtomicReference<Integer> status = new AtomicReference<>(200);

    private final AtomicReference<String> body = new AtomicReference<>("{\"usd\":1500}");

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/rates",
                exchange -> {
                    requests.incrementAndGet();
                    authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(status.get(), bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsSecretsInHeadersAndParsesTheJson() {
        when(secretService.resolveDecrypted(WORKSPACE_ID, "RATES_KEY"))
                .thenReturn(Optional.of("abc123"));

        FetchResult result =
                fetcher()
                        .fetchUncached(
                                WORKSPACE_ID,
                                url(),
                                List.of(
                                        new KnowledgeSourceHeader(
                                                "Authorization", "Bearer {{secrets.RATES_KEY}}")))
                        .join();

        assertThat(result.failed()).isFalse();
        assertThat(result.json().path("usd").asInt()).isEqualTo(1500);
        assertThat(authorization.get()).isEqualTo("Bearer abc123");
    }

    @Test
    void reusesAResponseUntilItsCacheTimeRunsOut() {
        KnowledgeSourceFetcher fetcher = fetcher();
        KnowledgeSource source = source(60);

        fetcher.fetch(source).join();
        fetcher.fetch(source).join();

        assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    void fetchesAgainAfterTheSourceChanges() {
        KnowledgeSourceFetcher fetcher = fetcher();
        KnowledgeSource source = source(60);

        fetcher.fetch(source).join();
        source.setUpdatedAt(source.getUpdatedAt().plusSeconds(1));
        fetcher.fetch(source).join();

        assertThat(requests.get()).isEqualTo(2);
    }

    @Test
    void doesNotKeepFailures() {
        KnowledgeSourceFetcher fetcher = fetcher();
        KnowledgeSource source = source(60);
        status.set(503);

        assertThat(fetcher.fetch(source).join().error()).isEqualTo("HTTP 503");

        status.set(200);

        assertThat(fetcher.fetch(source).join().failed()).isFalse();
        assertThat(requests.get()).isEqualTo(2);
    }

    @Test
    void reportsAResponseThatIsNotJson() {
        body.set("<html>oops</html>");

        FetchResult result = fetcher().fetchUncached(WORKSPACE_ID, url(), List.of()).join();

        assertThat(result.error()).isEqualTo("The response isn't JSON");
        assertThat(result.body()).isEqualTo("<html>oops</html>");
    }

    @Test
    void reportsAnUnknownSecretWithoutSendingTheRequest() {
        when(secretService.resolveDecrypted(WORKSPACE_ID, "MISSING")).thenReturn(Optional.empty());

        FetchResult result =
                fetcher()
                        .fetchUncached(WORKSPACE_ID, url() + "?key={{secrets.MISSING}}", List.of())
                        .join();

        assertThat(result.error()).isEqualTo("Unknown secret: MISSING");
        assertThat(requests.get()).isZero();
    }

    private KnowledgeSourceFetcher fetcher() {
        return new KnowledgeSourceFetcher(new ObjectMapper(), secretService);
    }

    private KnowledgeSource source(int cacheSeconds) {
        KnowledgeSource source = new KnowledgeSource();
        source.setId(UUID.randomUUID());
        source.setWorkspaceId(WORKSPACE_ID);
        source.setName("rates");
        source.setUrl(url());
        source.setCacheSeconds(cacheSeconds);
        source.setUpdatedAt(Instant.now());

        return source;
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/rates";
    }
}
