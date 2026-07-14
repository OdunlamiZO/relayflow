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
            String systemPrompt =
                    request.systemPrompt()
                            + LlmPrompts.buildFormatInstruction(request.extractionFields());
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

                return AgentLlmResponseParser.empty();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String content =
                    root.path("choices").path(0).path("message").path("content").asText("");

            return AgentLlmResponseParser.parse(objectMapper, content);
        } catch (Exception e) {
            log.error("LLM call failed for provider={}: {}", provider(), e.getMessage(), e);

            return AgentLlmResponseParser.empty();
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
}
