package com.relayflow.api.agent.llm;

public interface LlmClient {

    LlmProvider provider();

    AgentLlmResponse complete(AgentLlmRequest request);
}
