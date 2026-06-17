package com.relayflow.api.agent.domain;

import java.util.UUID;

public record WorkflowMapping(UUID workflowId, String name, String triggerDescription) {}
