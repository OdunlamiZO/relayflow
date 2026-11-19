package io.odunlamizo.relayflow.plugins.openaicompatible;

// Docs: docs/codebase-components.md#official-plugin-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.odunlamizo.relayflow.plugin.ChatMessage;
import io.odunlamizo.relayflow.plugin.ChatRequest;
import io.odunlamizo.relayflow.plugin.ProviderException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleChatModelProviderTest {

    private final AtomicReference<String> receivedBody = new AtomicReference<>();

    private final AtomicReference<String> receivedAuthorization = new AtomicReference<>();

    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1/chat/completions",
                exchange -> {
                    receivedAuthorization.set(
                            exchange.getRequestHeaders().getFirst("authorization"));
                    receivedBody.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8));
                    byte[] bytes =
                            "{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}"
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
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
    void sendsTheSystemPromptFirstAndAsksForJson() {
        String reply =
                provider("gsk-test")
                        .complete(
                                new ChatRequest(
                                        "Be helpful",
                                        List.of(new ChatMessage("user", "Hello")),
                                        "llama-3.3-70b"));

        assertThat(reply).isEqualTo("{}");
        assertThat(receivedAuthorization.get()).isEqualTo("Bearer gsk-test");
        assertThat(receivedBody.get())
                .contains("\"model\":\"llama-3.3-70b\"")
                .contains(
                        "\"messages\":[{\"role\":\"system\",\"content\":\"Be helpful\"},"
                                + "{\"role\":\"user\",\"content\":\"Hello\"}]")
                .contains("\"response_format\":{\"type\":\"json_object\"}")
                .doesNotContain("require_parameters");
    }

    @Test
    void sendsNoAuthorizationHeaderWithoutAKey() {
        provider("").complete(new ChatRequest("", List.of(), "llama3.2"));

        assertThat(receivedAuthorization.get()).isNull();
    }

    @Test
    void aMissingModelThrows() {
        assertThatThrownBy(() -> provider("").complete(new ChatRequest("", List.of(), " ")))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("platform:llm:openai-compatible:model");
    }

    @Test
    void reportsAMissingBaseUrlAsAConfigurationProblem() {
        OpenAiCompatibleChatModelProvider withoutBaseUrl = new OpenAiCompatibleChatModelProvider();
        withoutBaseUrl.initialize(name -> Optional.empty());

        assertThat(withoutBaseUrl.configurationProblem())
                .hasValueSatisfying(
                        problem -> assertThat(problem).contains("OPENAI_COMPATIBLE_BASE_URL"));
        assertThat(provider("").configurationProblem()).isEmpty();
    }

    private OpenAiCompatibleChatModelProvider provider(String apiKey) {
        Map<String, String> settings =
                Map.of(
                        "OPENAI_COMPATIBLE_BASE_URL",
                        "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/",
                        "OPENAI_COMPATIBLE_API_KEY",
                        apiKey);
        OpenAiCompatibleChatModelProvider provider = new OpenAiCompatibleChatModelProvider();
        provider.initialize(name -> Optional.ofNullable(settings.get(name)));

        return provider;
    }
}
