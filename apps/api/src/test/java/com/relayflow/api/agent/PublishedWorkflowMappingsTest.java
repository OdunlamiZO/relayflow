package com.relayflow.api.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relayflow.api.agent.domain.WorkflowMapping;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishedWorkflowMappingsTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    @Mock private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Test
    void keepsOnlyMappingsToPublishedWorkflows() {
        WorkflowMapping published = new WorkflowMapping(UUID.randomUUID(), "Published", "");
        WorkflowMapping unpublished = new WorkflowMapping(UUID.randomUUID(), "Unpublished", "");
        when(workflowDefinitionRepository.findEnabledIdsByWorkspace(WORKSPACE_ID))
                .thenReturn(Set.of(published.workflowId()));

        List<WorkflowMapping> result =
                new PublishedWorkflowMappings(workflowDefinitionRepository)
                        .filter(WORKSPACE_ID, List.of(published, unpublished));

        assertThat(result).containsExactly(published);
    }

    @Test
    void skipsTheLookupWhenThereAreNoMappings() {
        List<WorkflowMapping> result =
                new PublishedWorkflowMappings(workflowDefinitionRepository)
                        .filter(WORKSPACE_ID, List.of());

        assertThat(result).isEmpty();
        verify(workflowDefinitionRepository, never()).findEnabledIdsByWorkspace(any());
    }
}
