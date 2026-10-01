package com.relayflow.api.agent;

import com.relayflow.api.agent.domain.WorkflowMapping;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PublishedWorkflowMappings {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    public PublishedWorkflowMappings(WorkflowDefinitionRepository workflowDefinitionRepository) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
    }

    public List<WorkflowMapping> filter(UUID workspaceId, List<WorkflowMapping> workflowMappings) {
        if (workflowMappings.isEmpty()) {
            return workflowMappings;
        }

        Set<UUID> publishedIds =
                workflowDefinitionRepository.findEnabledIdsByWorkspace(workspaceId);

        return workflowMappings.stream()
                .filter(mapping -> publishedIds.contains(mapping.workflowId()))
                .toList();
    }
}
