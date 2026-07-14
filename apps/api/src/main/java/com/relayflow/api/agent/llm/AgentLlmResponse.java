package com.relayflow.api.agent.llm;

import java.util.List;
import java.util.Map;

public record AgentLlmResponse(
        String reply,
        String confidence,
        List<String> suggestedActions,
        boolean escalate,
        boolean needsClarification,
        Map<String, String> extractedData) {}
