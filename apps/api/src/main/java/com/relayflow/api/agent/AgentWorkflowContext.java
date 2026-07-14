package com.relayflow.api.agent;

import com.relayflow.api.agent.domain.ExtractionField;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the workflow variables seeded when the AI agent is the one triggering a run.
 *
 * <p>Only keys matching a field the workspace has configured are exposed as {@code agent.data.*}
 * variables — the LLM's {@code extractedData} is free-form model output, and an unconfigured (or
 * hallucinated) key must not leak into a workflow's variable namespace.
 */
final class AgentWorkflowContext {

    static Map<String, String> build(
            String reply,
            String confidence,
            Map<String, String> extractedData,
            List<ExtractionField> extractionFields) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("agent.reply", reply != null ? reply : "");
        if (confidence != null) {
            context.put("agent.confidence", confidence);
        }

        Set<String> knownKeys =
                extractionFields.stream().map(ExtractionField::key).collect(Collectors.toSet());

        for (Map.Entry<String, String> entry : extractedData.entrySet()) {
            if (knownKeys.contains(entry.getKey())) {
                context.put("agent.data." + entry.getKey(), entry.getValue());
            }
        }

        return context;
    }

    private AgentWorkflowContext() {}
}
