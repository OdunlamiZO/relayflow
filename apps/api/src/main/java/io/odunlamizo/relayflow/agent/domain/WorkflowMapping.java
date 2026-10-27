package io.odunlamizo.relayflow.agent.domain;

import java.util.UUID;

public record WorkflowMapping(UUID workflowId, String name, String triggerDescription) {}
