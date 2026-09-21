package com.relayflow.api.agent.llm;

import java.util.List;
import java.util.Map;

/**
 * {@code failed} is true when the LLM call itself errored — network, auth, HTTP status, or an
 * unparseable body — as opposed to a normal response the model happened to leave empty.
 */
public record AgentLlmResponse(
        String reply,
        String confidence,
        List<String> suggestedActions,
        boolean escalate,
        boolean needsClarification,
        Map<String, String> extractedData,
        boolean failed) {}
