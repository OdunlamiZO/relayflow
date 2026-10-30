package io.odunlamizo.relayflow.agent.domain;

// Docs: docs/codebase-components.md#ai-agent-domain-records

public enum AiAgentInvocationStatus {
    RUNNING,
    CLARIFYING,
    ESCALATED,
    DRAFTED,
    SENT,
    FAILED
}
