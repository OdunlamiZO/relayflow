package io.odunlamizo.relayflow.agent.dto;

// Docs: docs/codebase-components.md#ai-agent-dto-records

import io.odunlamizo.relayflow.agent.domain.AutonomyCeiling;
import io.odunlamizo.relayflow.agent.domain.ExtractionField;
import io.odunlamizo.relayflow.agent.domain.KnowledgeEntry;
import io.odunlamizo.relayflow.agent.domain.WorkflowMapping;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiAgentConfigurationResponse(
        UUID id,
        UUID workspaceId,
        String name,
        boolean isDefault,
        boolean enabled,
        AutonomyCeiling autonomyCeiling,
        String llmProvider,
        String instructions,
        List<KnowledgeEntry> knowledgeBase,
        List<String> escalationKeywords,
        List<WorkflowMapping> workflowMappings,
        List<ExtractionField> extractionFields,
        Instant createdAt,
        Instant updatedAt) {}
