package com.relayflow.api.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class AgentLlmResponseParser {

    static AgentLlmResponse parse(ObjectMapper objectMapper, String rawJson) {
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

            Map<String, String> extractedData = new LinkedHashMap<>();
            JsonNode extractedDataNode = node.path("extractedData");
            if (extractedDataNode.isObject()) {
                Iterator<Entry<String, JsonNode>> fields = extractedDataNode.fields();
                while (fields.hasNext()) {
                    Entry<String, JsonNode> field = fields.next();
                    extractedData.put(field.getKey(), field.getValue().asText(""));
                }
            }

            return new AgentLlmResponse(
                    reply,
                    confidence,
                    suggestedActions,
                    escalate,
                    needsClarification,
                    extractedData);
        } catch (Exception e) {

            return empty();
        }
    }

    static AgentLlmResponse empty() {

        return new AgentLlmResponse("", null, List.of(), false, false, Map.of());
    }

    private AgentLlmResponseParser() {}
}
