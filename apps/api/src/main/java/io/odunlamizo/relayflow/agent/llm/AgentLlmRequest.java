package io.odunlamizo.relayflow.agent.llm;

import io.odunlamizo.relayflow.agent.domain.ExtractionField;
import java.util.List;

public record AgentLlmRequest(
        String systemPrompt,
        List<LlmMessage> messages,
        String model,
        List<ExtractionField> extractionFields) {}
