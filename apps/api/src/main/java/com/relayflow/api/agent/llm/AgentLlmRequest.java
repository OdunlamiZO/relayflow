package com.relayflow.api.agent.llm;

import com.relayflow.api.agent.domain.ExtractionField;
import java.util.List;

public record AgentLlmRequest(
        String systemPrompt,
        List<LlmMessage> messages,
        String model,
        List<ExtractionField> extractionFields) {}
