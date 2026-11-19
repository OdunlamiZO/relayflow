package io.odunlamizo.relayflow.plugins.anthropic;

// Docs: docs/codebase-components.md#anthropicchatmodelprovider

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.odunlamizo.relayflow.plugin.ChatMessage;
import io.odunlamizo.relayflow.plugin.ChatModelProvider;
import io.odunlamizo.relayflow.plugin.ChatRequest;
import io.odunlamizo.relayflow.plugin.ProviderException;
import io.odunlamizo.relayflow.plugin.ProviderSettings;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AnthropicChatModelProvider implements ChatModelProvider {

    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";

    private static final String DEFAULT_MODEL = "claude-haiku-4-5";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String apiKey = "";

    private String baseUrl = DEFAULT_BASE_URL;

    @Override
    public String id() {
        return "ANTHROPIC";
    }

    @Override
    public String displayName() {
        return "Anthropic";
    }

    @Override
    public void initialize(ProviderSettings settings) {
        apiKey = settings.get("ANTHROPIC_API_KEY").map(String::strip).orElse("");
        baseUrl =
                settings.get("ANTHROPIC_BASE_URL")
                        .map(url -> url.strip().replaceAll("/+$", ""))
                        .filter(url -> !url.isEmpty())
                        .orElse(DEFAULT_BASE_URL);
    }

    @Override
    public String complete(ChatRequest request) {
        if (apiKey.isEmpty()) {
            throw new ProviderException("Set ANTHROPIC_API_KEY to use the Anthropic provider");
        }

        String model =
                request.model() != null && !request.model().isBlank()
                        ? request.model()
                        : DEFAULT_MODEL;

        HttpRequest httpRequest =
                HttpRequest.newBuilder(URI.create(baseUrl + "/v1/messages"))
                        .timeout(REQUEST_TIMEOUT)
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .header("content-type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(buildBody(model, request)))
                        .build();

        HttpResponse<String> response = send(httpRequest);

        if (response.statusCode() >= 400) {
            throw new ProviderException(
                    "Anthropic returned " + response.statusCode() + ": " + response.body());
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());

            return root.path("content").path(0).path("text").asText("");
        } catch (Exception exception) {
            throw new ProviderException("Anthropic returned an unreadable response", exception);
        }
    }

    private String buildBody(String model, ChatRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 4096);
        body.put("system", request.systemPrompt());

        ArrayNode messages = body.putArray("messages");
        for (ChatMessage message : request.messages()) {
            messages.addObject().put("role", message.role()).put("content", message.content());
        }

        return body.toString();
    }

    private HttpResponse<String> send(HttpRequest httpRequest) {
        try {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new ProviderException("Anthropic request was interrupted", exception);
        } catch (Exception exception) {
            throw new ProviderException("Could not reach Anthropic", exception);
        }
    }
}
