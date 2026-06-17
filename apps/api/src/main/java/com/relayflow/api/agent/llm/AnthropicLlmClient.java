package com.relayflow.api.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AnthropicLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmClient.class);

    private static final String DEFAULT_MODEL = "claude-haiku-4-5";

    private static final String MODEL_REDIS_KEY = "platform:llm:anthropic:model";

    @Value("${relayflow.llm.anthropic.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper;

    private final LlmPlatformConfigService configService;

    private RestClient restClient;

    public AnthropicLlmClient(ObjectMapper objectMapper, LlmPlatformConfigService configService) {
        this.objectMapper = objectMapper;
        this.configService = configService;
    }

    @PostConstruct
    void init() {
        restClient =
                RestClient.builder()
                        .baseUrl("https://api.anthropic.com")
                        .defaultHeader("x-api-key", apiKey)
                        .defaultHeader("anthropic-version", "2023-06-01")
                        .defaultHeader("content-type", "application/json")
                        .build();
    }

    @Override
    public LlmProvider provider() {

        return LlmProvider.ANTHROPIC;
    }

    @Override
    public AgentLlmResponse complete(AgentLlmRequest request) {
        try {
            String model =
                    (request.model() != null && !request.model().isBlank())
                            ? request.model()
                            : configService.getConfigValue(MODEL_REDIS_KEY, DEFAULT_MODEL);

            String systemPrompt = request.systemPrompt() + LlmPrompts.JSON_FORMAT_INSTRUCTION;

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 4096);
            body.put("system", systemPrompt);

            ArrayNode messages = body.putArray("messages");
            for (LlmMessage msg : request.messages()) {
                ObjectNode m = messages.addObject();
                m.put("role", msg.role());
                m.put("content", msg.content());
            }

            String raw =
                    restClient.post().uri("/v1/messages").body(body).retrieve().body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("content").path(0).path("text").asText("");

            return parseResponse(text);
        } catch (Exception e) {
            log.warn("Anthropic LLM call failed: {}", e.getMessage());

            return emptyResponse();
        }
    }

    private AgentLlmResponse parseResponse(String rawJson) {
        try {
            String json = rawJson.trim();
            if (json.startsWith("```")) {
                int newline = json.indexOf('\n');
                int closing = json.lastIndexOf("```");
                if (newline > 0 && closing > newline) {
                    json = json.substring(newline + 1, closing).trim();
                }
            }

            JsonNode node = objectMapper.readTree(json);
            String reply = node.path("reply").asText("");
            JsonNode confidenceNode = node.path("confidence");
            String confidence = confidenceNode.isNull() ? null : confidenceNode.asText(null);
            boolean escalate = node.path("escalate").asBoolean(false);
            boolean needsClarification = node.path("needsClarification").asBoolean(false);

            List<String> suggestedActions = new ArrayList<>();
            JsonNode actionsNode = node.path("suggestedActions");
            if (actionsNode.isArray()) {
                actionsNode.forEach(actionNode -> suggestedActions.add(actionNode.asText()));
            }

            return new AgentLlmResponse(
                    reply, confidence, suggestedActions, escalate, needsClarification);
        } catch (Exception e) {
            return emptyResponse();
        }
    }

    private AgentLlmResponse emptyResponse() {

        return new AgentLlmResponse("", null, List.of(), false, false);
    }
}
