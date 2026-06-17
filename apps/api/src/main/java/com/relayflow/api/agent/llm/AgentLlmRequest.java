package com.relayflow.api.agent.llm;

import java.util.List;

public record AgentLlmRequest(String systemPrompt, List<LlmMessage> messages, String model) {}
