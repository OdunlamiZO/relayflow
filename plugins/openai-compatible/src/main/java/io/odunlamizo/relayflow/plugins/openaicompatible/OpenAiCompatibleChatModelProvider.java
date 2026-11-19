package io.odunlamizo.relayflow.plugins.openaicompatible;

// Docs: docs/codebase-components.md#openaicompatiblechatmodelprovider

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
import java.util.Optional;

public class OpenAiCompatibleChatModelProvider implements ChatModelProvider {

    private static final String OPENROUTER_HOST = "openrouter.ai";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl = "";

    private String apiKey = "";

    @Override
    public String id() {
        return "OPENAI_COMPATIBLE";
    }

    @Override
    public String displayName() {
        return "OpenAI-compatible";
    }

    @Override
    public void initialize(ProviderSettings settings) {
        baseUrl =
                settings.get("OPENAI_COMPATIBLE_BASE_URL")
                        .map(url -> url.strip().replaceAll("/+$", ""))
                        .orElse("");
        apiKey = settings.get("OPENAI_COMPATIBLE_API_KEY").map(String::strip).orElse("");
    }

    @Override
    public Optional<String> configurationProblem() {
        return baseUrl.isEmpty()
                ? Optional.of(
                        "Set OPENAI_COMPATIBLE_BASE_URL to use the OpenAI-compatible provider")
                : Optional.empty();
    }

    @Override
    public String complete(ChatRequest request) {
        Optional<String> problem = configurationProblem();

        if (problem.isPresent()) {
            throw new ProviderException(problem.get());
        }

        if (request.model() == null || request.model().isBlank()) {
            throw new ProviderException(
                    "Choose a model for OpenAI-compatible in Settings → Providers, or set the"
                            + " platform:llm:openai-compatible:model Redis key");
        }

        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                        .timeout(REQUEST_TIMEOUT)
                        .header("content-type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(buildBody(request)));

        if (!apiKey.isEmpty()) {
            builder.header("authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = send(builder.build());

        if (response.statusCode() >= 400) {
            throw new ProviderException(
                    "The OpenAI-compatible API returned "
                            + response.statusCode()
                            + ": "
                            + response.body());
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());

            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception exception) {
            throw new ProviderException(
                    "The OpenAI-compatible API returned an unreadable response", exception);
        }
    }

    private String buildBody(ChatRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", request.model());

        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", request.systemPrompt());
        for (ChatMessage message : request.messages()) {
            messages.addObject().put("role", message.role()).put("content", message.content());
        }

        body.putObject("response_format").put("type", "json_object");

        // OpenRouter only: skips hosts that ignore response_format; other APIs reject this field.
        if (OPENROUTER_HOST.equals(URI.create(baseUrl).getHost())) {
            body.putObject("provider").put("require_parameters", true);
        }

        return body.toString();
    }

    private HttpResponse<String> send(HttpRequest httpRequest) {
        try {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new ProviderException("The OpenAI-compatible request was interrupted", exception);
        } catch (Exception exception) {
            throw new ProviderException("Could not reach the OpenAI-compatible API", exception);
        }
    }
}
