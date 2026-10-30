package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#llmclient

public interface LlmClient {

    LlmProvider provider();

    AgentLlmResponse complete(AgentLlmRequest request);
}
