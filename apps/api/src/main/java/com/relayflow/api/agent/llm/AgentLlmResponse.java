package com.relayflow.api.agent.llm;

import java.util.List;

public record AgentLlmResponse(
        String reply,
        String confidence,
        List<String> suggestedActions,
        boolean escalate,
        boolean needsClarification) {}
