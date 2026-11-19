package io.odunlamizo.relayflow.plugins.anthropic;

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

class AnthropicChatModelProviderTest {

    private final AtomicReference<String> receivedBody = new AtomicReference<>();

    private final AtomicReference<String> receivedApiKey = new AtomicReference<>();

    private HttpServer server;

    private int responseStatus = 200;

    private String responseBody =
            "{\"content\":[{\"type\":\"text\",\"text\":\"{\\\"reply\\\":\\\"Hi\\\"}\"}]}";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1/messages",
                exchange -> {
                    receivedApiKey.set(exchange.getRequestHeaders().getFirst("x-api-key"));
                    receivedBody.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8));
                    byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(responseStatus, bytes.length);
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
    void sendsThePromptAndReturnsTheReplyText() {
        AnthropicChatModelProvider provider = provider("sk-test");

        String reply =
                provider.complete(
                        new ChatRequest(
                                "Be helpful", List.of(new ChatMessage("user", "Hello")), null));

        assertThat(reply).isEqualTo("{\"reply\":\"Hi\"}");
        assertThat(receivedApiKey.get()).isEqualTo("sk-test");
        assertThat(receivedBody.get())
                .contains("\"model\":\"claude-haiku-4-5\"")
                .contains("\"system\":\"Be helpful\"")
                .contains("\"role\":\"user\",\"content\":\"Hello\"");
    }

    @Test
    void usesTheRequestedModel() {
        provider("sk-test").complete(new ChatRequest("", List.of(), "claude-sonnet-4-5"));

        assertThat(receivedBody.get()).contains("\"model\":\"claude-sonnet-4-5\"");
    }

    @Test
    void anErrorStatusThrows() {
        responseStatus = 401;
        responseBody = "{\"error\":\"invalid key\"}";

        assertThatThrownBy(() -> provider("sk-test").complete(new ChatRequest("", List.of(), null)))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("401");
    }

    @Test
    void aMissingApiKeyThrows() {
        assertThatThrownBy(() -> provider("").complete(new ChatRequest("", List.of(), null)))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");
    }

    private AnthropicChatModelProvider provider(String apiKey) {
        Map<String, String> settings =
                Map.of(
                        "ANTHROPIC_API_KEY",
                        apiKey,
                        "ANTHROPIC_BASE_URL",
                        "http://127.0.0.1:" + server.getAddress().getPort() + "/");
        AnthropicChatModelProvider provider = new AnthropicChatModelProvider();
        provider.initialize(name -> Optional.ofNullable(settings.get(name)));

        return provider;
    }
}
