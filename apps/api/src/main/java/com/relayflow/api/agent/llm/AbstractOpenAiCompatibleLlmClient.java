package com.relayflow.api.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

abstract class AbstractOpenAiCompatibleLlmClient implements LlmClient {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final ObjectMapper objectMapper;

    AbstractOpenAiCompatibleLlmClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected AgentLlmResponse doComplete(
            RestClient restClient, String model, AgentLlmRequest request) {
        try {
            String systemPrompt = request.systemPrompt() + LlmPrompts.JSON_FORMAT_INSTRUCTION;
            List<Map<String, String>> messages = buildMessages(systemPrompt, request.messages());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("messages", messages);
            payload.put("response_format", Map.of("type", "json_object"));

            String responseBody =
                    restClient
                            .post()
                            .uri("/v1/chat/completions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(objectMapper.writeValueAsString(payload))
                            .retrieve()
                            .body(String.class);

            if (responseBody == null) {

                return emptyResponse();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String content =
                    root.path("choices").path(0).path("message").path("content").asText("");

            return parseAgentResponse(content);
        } catch (Exception e) {
            log.error("LLM call failed for provider={}: {}", provider(), e.getMessage(), e);

            return emptyResponse();
        }
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, List<LlmMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>(history.size() + 1);
        messages.add(Map.of("role", "system", "content", systemPrompt));

        for (LlmMessage msg : history) {
            messages.add(Map.of("role", msg.role(), "content", msg.content()));
        }

        return messages;
    }

    private AgentLlmResponse parseAgentResponse(String rawJson) {
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

    private static AgentLlmResponse emptyResponse() {
        return new AgentLlmResponse("", null, List.of(), false, false);
    }
}
