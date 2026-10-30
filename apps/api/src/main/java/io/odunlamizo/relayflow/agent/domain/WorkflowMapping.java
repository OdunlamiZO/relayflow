package io.odunlamizo.relayflow.agent.domain;

// Docs: docs/codebase-components.md#ai-agent-domain-records

import java.util.UUID;

public record WorkflowMapping(UUID workflowId, String name, String triggerDescription) {}
