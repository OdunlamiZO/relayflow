package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#openaicompatiblellmclient

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls any OpenAI-compatible chat completions API — OpenAI, Groq, OpenRouter, Together, DeepSeek,
 * Mistral, Ollama, vLLM, LM Studio — at the base URL the administrator configures.
 */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private static final String MODEL_REDIS_KEY = "platform:llm:openai-compatible:model";

    private static final String OPENROUTER_HOST = "openrouter.ai";

    private final ObjectMapper objectMapper;

    private final LlmPlatformConfigService configService;

    private final String baseUrl;

    private final RestClient restClient;

    public OpenAiCompatibleLlmClient(
            ObjectMapper objectMapper,
            LlmPlatformConfigService configService,
            RestClient.Builder restClientBuilder,
            @Value("${relayflow.llm.openai-compatible.base-url}") String baseUrl,
            @Value("${relayflow.llm.openai-compatible.api-key}") String apiKey) {
        this.objectMapper = objectMapper;
        this.configService = configService;
        this.baseUrl = baseUrl.strip().replaceAll("/+$", "");

        if (!apiKey.isBlank()) {
            restClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.strip());
        }

        this.restClient = restClientBuilder.build();
    }

    @Override
    public LlmProvider provider() {

        return LlmProvider.OPENAI_COMPATIBLE;
    }

    @Override
    public AgentLlmResponse complete(AgentLlmRequest request) {
        String model =
                (request.model() != null && !request.model().isBlank())
                        ? request.model()
                        : configService.getConfigValue(MODEL_REDIS_KEY, "");

        if (baseUrl.isBlank() || model.isBlank()) {
            log.error(
                    "OpenAI-compatible LLM provider isn't configured — set OPENAI_COMPATIBLE_BASE_URL"
                            + " and the platform:llm:openai-compatible:model Redis key {} {}",
                    kv("baseUrlSet", !baseUrl.isBlank()),
                    kv("modelSet", !model.isBlank()));

            return AgentLlmResponseParser.failure();
        }

        try {
            String responseBody =
                    restClient
                            .post()
                            .uri(baseUrl + "/chat/completions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(objectMapper.writeValueAsString(buildPayload(model, request)))
                            .retrieve()
                            .body(String.class);

            if (responseBody == null) {

                return AgentLlmResponseParser.failure();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String content =
                    root.path("choices").path(0).path("message").path("content").asText("");

            return AgentLlmResponseParser.parse(objectMapper, content);
        } catch (Exception exception) {
            log.error(
                    "LLM call failed {} {}",
                    kv("provider", provider()),
                    kv("model", model),
                    exception);

            return AgentLlmResponseParser.failure();
        }
    }

    private Map<String, Object> buildPayload(String model, AgentLlmRequest request) {
        String systemPrompt =
                request.systemPrompt()
                        + LlmPrompts.buildFormatInstruction(request.extractionFields());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", buildMessages(systemPrompt, request.messages()));
        payload.put("response_format", Map.of("type", "json_object"));

        // OpenRouter may otherwise route to a host that ignores response_format and replies in
        // prose, which the parser can't read. Other APIs reject the unknown field, so it's
        // OpenRouter-only.
        if (OPENROUTER_HOST.equals(URI.create(baseUrl).getHost())) {
            payload.put("provider", Map.of("require_parameters", true));
        }

        return payload;
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, List<LlmMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>(history.size() + 1);
        messages.add(Map.of("role", "system", "content", systemPrompt));

        for (LlmMessage message : history) {
            messages.add(Map.of("role", message.role(), "content", message.content()));
        }

        return messages;
    }
}
