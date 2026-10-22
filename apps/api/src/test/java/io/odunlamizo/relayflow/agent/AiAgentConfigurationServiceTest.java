package io.odunlamizo.relayflow.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.odunlamizo.relayflow.agent.domain.AiAgentConfiguration;
import io.odunlamizo.relayflow.agent.domain.AutonomyCeiling;
import io.odunlamizo.relayflow.agent.domain.ExtractionField;
import io.odunlamizo.relayflow.agent.dto.UpdateAiAgentConfigurationRequest;
import io.odunlamizo.relayflow.agent.repository.AiAgentConfigurationRepository;
import io.odunlamizo.relayflow.agent.repository.ConversationAiDraftRepository;
import io.odunlamizo.relayflow.channel.repository.ChannelAccountRepository;
import io.odunlamizo.relayflow.hook.HookService;
import io.odunlamizo.relayflow.messaging.MessagingService;
import io.odunlamizo.relayflow.messaging.repository.ConversationRepository;
import io.odunlamizo.relayflow.workflow.engine.WorkflowEngineService;
import io.odunlamizo.relayflow.workflow.repository.WorkflowDefinitionRepository;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AiAgentConfigurationServiceTest {

    @Mock private AiAgentConfigurationRepository configurationRepository;

    @Mock private ConversationAiDraftRepository draftRepository;

    @Mock private WorkspaceRepository workspaceRepository;

    @Mock private ChannelAccountRepository channelAccountRepository;

    @Mock private ConversationRepository conversationRepository;

    @Mock private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Mock private WorkflowEngineService workflowEngineService;

    @Mock private MessagingService messagingService;

    @Mock private HookService hookService;

    private final UUID workspaceId = UUID.randomUUID();

    private final UUID channelAccountId = UUID.randomUUID();

    private AiAgentConfigurationService service() {
        return new AiAgentConfigurationService(
                configurationRepository,
                draftRepository,
                workspaceRepository,
                channelAccountRepository,
                conversationRepository,
                workflowDefinitionRepository,
                workflowEngineService,
                messagingService,
                hookService);
    }

    private AiAgentConfiguration configuration(boolean enabled) {
        AiAgentConfiguration configuration = new AiAgentConfiguration();
        configuration.setEnabled(enabled);

        return configuration;
    }

    @Test
    void resolveEnabledConfigurationPrefersTheChannelsOwnEnabledAssignment() {
        AiAgentConfiguration assigned = configuration(true);
        when(channelAccountRepository.findAiAgentConfiguration(channelAccountId))
                .thenReturn(Optional.of(assigned));

        Optional<AiAgentConfiguration> result =
                service().resolveEnabledConfiguration(workspaceId, channelAccountId);

        assertThat(result).contains(assigned);
    }

    @Test
    void resolveEnabledConfigurationDoesNotFallBackWhenTheAssignedConfigIsDisabled() {
        when(channelAccountRepository.findAiAgentConfiguration(channelAccountId))
                .thenReturn(Optional.of(configuration(false)));

        Optional<AiAgentConfiguration> result =
                service().resolveEnabledConfiguration(workspaceId, channelAccountId);

        assertThat(result).isEmpty();
    }

    @Test
    void resolveEnabledConfigurationFallsBackToTheWorkspaceDefaultWhenUnassigned() {
        AiAgentConfiguration defaultConfig = configuration(true);
        when(channelAccountRepository.findAiAgentConfiguration(channelAccountId))
                .thenReturn(Optional.empty());
        when(configurationRepository.findDefault(workspaceId))
                .thenReturn(Optional.of(defaultConfig));

        Optional<AiAgentConfiguration> result =
                service().resolveEnabledConfiguration(workspaceId, channelAccountId);

        assertThat(result).contains(defaultConfig);
    }

    @Test
    void resolveEnabledConfigurationIsEmptyWhenUnassignedAndTheDefaultIsDisabled() {
        when(channelAccountRepository.findAiAgentConfiguration(any())).thenReturn(Optional.empty());
        when(configurationRepository.findDefault(workspaceId))
                .thenReturn(Optional.of(configuration(false)));

        Optional<AiAgentConfiguration> result =
                service().resolveEnabledConfiguration(workspaceId, channelAccountId);

        assertThat(result).isEmpty();
    }

    @Test
    void resolveEnabledConfigurationIsEmptyWhenNeitherIsConfigured() {
        when(channelAccountRepository.findAiAgentConfiguration(any())).thenReturn(Optional.empty());
        when(configurationRepository.findDefault(any())).thenReturn(Optional.empty());

        assertThat(service().resolveEnabledConfiguration(workspaceId, channelAccountId)).isEmpty();
    }

    @Test
    void updateRejectsAnExtractionFieldWhoseHookDoesNotExist() {
        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        AiAgentConfiguration configuration = configuration(true);
        configuration.setWorkspace(workspace);
        UUID configurationId = UUID.randomUUID();

        when(configurationRepository.findInWorkspace(configurationId, workspaceId))
                .thenReturn(Optional.of(configuration));
        when(hookService.hookExists(workspaceId, "hook:missing")).thenReturn(false);

        UpdateAiAgentConfigurationRequest request =
                new UpdateAiAgentConfigurationRequest(
                        "Support",
                        true,
                        AutonomyCeiling.DRAFT_ONLY,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(new ExtractionField("email", "Email", "hook:missing")));

        assertThatThrownBy(
                        () -> service().updateConfiguration(workspaceId, configurationId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("'email'");

        verify(configurationRepository, never()).save(any());
    }
}
