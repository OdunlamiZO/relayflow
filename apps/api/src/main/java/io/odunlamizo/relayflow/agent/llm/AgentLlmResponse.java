package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#agentllmresponse

import java.util.List;
import java.util.Map;

/** failed is true when the LLM call errored or its body could not be parsed. */
public record AgentLlmResponse(
        String reply,
        String confidence,
        List<String> suggestedActions,
        boolean escalate,
        boolean needsClarification,
        Map<String, String> extractedData,
        boolean failed) {}
