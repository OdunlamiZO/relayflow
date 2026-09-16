package com.relayflow.api.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.relayflow.api.agent.domain.AiAgentConfiguration;
import com.relayflow.api.agent.repository.AiAgentConfigurationRepository;
import com.relayflow.api.agent.repository.ConversationAiDraftRepository;
import com.relayflow.api.channel.repository.ChannelAccountRepository;
import com.relayflow.api.messaging.MessagingService;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.workflow.engine.WorkflowEngineService;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import com.relayflow.api.workspace.repository.WorkspaceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
                messagingService);
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
        when(configurationRepository.findByWorkspaceIdAndDefaultConfigTrue(workspaceId))
                .thenReturn(Optional.of(defaultConfig));

        Optional<AiAgentConfiguration> result =
                service().resolveEnabledConfiguration(workspaceId, channelAccountId);

        assertThat(result).contains(defaultConfig);
    }

    @Test
    void resolveEnabledConfigurationIsEmptyWhenUnassignedAndTheDefaultIsDisabled() {
        when(channelAccountRepository.findAiAgentConfiguration(any())).thenReturn(Optional.empty());
        when(configurationRepository.findByWorkspaceIdAndDefaultConfigTrue(workspaceId))
                .thenReturn(Optional.of(configuration(false)));

        Optional<AiAgentConfiguration> result =
                service().resolveEnabledConfiguration(workspaceId, channelAccountId);

        assertThat(result).isEmpty();
    }

    @Test
    void resolveEnabledConfigurationIsEmptyWhenNeitherIsConfigured() {
        when(channelAccountRepository.findAiAgentConfiguration(any())).thenReturn(Optional.empty());
        when(configurationRepository.findByWorkspaceIdAndDefaultConfigTrue(any()))
                .thenReturn(Optional.empty());

        assertThat(service().resolveEnabledConfiguration(workspaceId, channelAccountId)).isEmpty();
    }
}
