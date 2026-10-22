package io.odunlamizo.relayflow.agent.llm;

public interface LlmClient {

    LlmProvider provider();

    AgentLlmResponse complete(AgentLlmRequest request);
}
