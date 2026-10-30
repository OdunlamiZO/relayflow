package io.odunlamizo.relayflow.agent.dto;

// Docs: docs/codebase-components.md#ai-agent-dto-records

import io.odunlamizo.relayflow.agent.llm.LlmProvider;

public record PlatformLlmProviderResponse(LlmProvider provider) {}
