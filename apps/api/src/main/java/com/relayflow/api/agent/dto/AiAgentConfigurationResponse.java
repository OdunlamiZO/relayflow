package com.relayflow.api.agent.dto;

import com.relayflow.api.agent.domain.AutonomyCeiling;
import com.relayflow.api.agent.domain.KnowledgeEntry;
import com.relayflow.api.agent.domain.WorkflowMapping;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiAgentConfigurationResponse(
        UUID id,
        UUID workspaceId,
        String name,
        boolean enabled,
        AutonomyCeiling autonomyCeiling,
        String instructions,
        List<KnowledgeEntry> knowledgeBase,
        List<String> escalationKeywords,
        List<WorkflowMapping> workflowMappings,
        Instant createdAt,
        Instant updatedAt) {}
