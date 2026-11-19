package io.odunlamizo.relayflow.providersettings;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.odunlamizo.relayflow.agent.llm.LlmClientFactory;
import io.odunlamizo.relayflow.agent.llm.LlmPlatformConfigService;
import io.odunlamizo.relayflow.audio.AudioProviderResolver;
import io.odunlamizo.relayflow.plugin.ChatModelProvider;
import io.odunlamizo.relayflow.pluginregistry.PluginRegistry;
import io.odunlamizo.relayflow.providersettings.dto.ProviderSettingsResponse;
import io.odunlamizo.relayflow.providersettings.dto.UpdateProviderSettingsRequest;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceProviderSettings;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ProviderSettingsServiceTest {

    private final PluginRegistry pluginRegistry = mock(PluginRegistry.class);

    private final LlmClientFactory llmClientFactory = mock(LlmClientFactory.class);

    private final LlmPlatformConfigService llmPlatformConfigService =
            mock(LlmPlatformConfigService.class);

    private final AudioProviderResolver audioProviderResolver = mock(AudioProviderResolver.class);

    private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);

    private final ChatModelProvider anthropic = mock(ChatModelProvider.class);

    private final ProviderSettingsService service =
            new ProviderSettingsService(
                    pluginRegistry,
                    llmClientFactory,
                    llmPlatformConfigService,
                    audioProviderResolver,
                    workspaceRepository);

    private final Workspace workspace = new Workspace();

    @BeforeEach
    void setUp() {
        when(anthropic.id()).thenReturn("ANTHROPIC");
        when(anthropic.displayName()).thenReturn("Anthropic");
        when(anthropic.configurationProblem()).thenReturn(Optional.empty());
        when(pluginRegistry.chatModelProviders()).thenReturn(List.of(anthropic));
        when(pluginRegistry.findChatModelProvider("ANTHROPIC")).thenReturn(Optional.of(anthropic));
        when(pluginRegistry.findChatModelProvider("GEMINI")).thenReturn(Optional.empty());
        when(llmClientFactory.instanceProvider()).thenReturn(Optional.of(anthropic));
        when(llmPlatformConfigService.getConfigValue("platform:llm:anthropic:model", null))
                .thenReturn("claude-haiku-4-5");
        when(workspaceRepository.save(any(Workspace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void listsTheWorkspaceChoiceNextToTheInstanceDefault() {
        workspace.setProviderSettings(
                new WorkspaceProviderSettings(
                        "ANTHROPIC",
                        Map.of("ANTHROPIC", "claude-sonnet-4-5"),
                        null,
                        Map.of(),
                        null));

        ProviderSettingsResponse response = service.getSettings(workspace);

        assertThat(response.chatModel().selected()).isEqualTo("ANTHROPIC");
        assertThat(response.chatModel().instanceDefault()).isEqualTo("ANTHROPIC");
        assertThat(response.chatModel().providers().getFirst().model())
                .isEqualTo("claude-sonnet-4-5");
        assertThat(response.chatModel().providers().getFirst().instanceModel())
                .isEqualTo("claude-haiku-4-5");
        assertThat(response.transcription().selected()).isNull();
    }

    @Test
    void reportsAPluginWhoseSettingsAreMissing() {
        when(anthropic.configurationProblem())
                .thenReturn(Optional.of("Set ANTHROPIC_API_KEY to use the Anthropic provider"));

        assertThat(service.getSettings(workspace).chatModel().providers().getFirst())
                .extracting("configurationProblem")
                .isEqualTo("Set ANTHROPIC_API_KEY to use the Anthropic provider");
    }

    @Test
    void savesTheChoiceAndDropsBlankModels() {
        service.updateSettings(
                workspace,
                new UpdateProviderSettingsRequest(
                        "ANTHROPIC",
                        Map.of("ANTHROPIC", " claude-sonnet-4-5 ", "OPENAI_COMPATIBLE", " "),
                        null,
                        null,
                        ""));

        assertThat(workspace.getProviderSettings())
                .isEqualTo(
                        new WorkspaceProviderSettings(
                                "ANTHROPIC",
                                Map.of("ANTHROPIC", "claude-sonnet-4-5"),
                                null,
                                Map.of(),
                                null));
    }

    @Test
    void rejectsAProviderWhosePluginIsNotInstalled() {
        UpdateProviderSettingsRequest request =
                new UpdateProviderSettingsRequest("GEMINI", null, null, null, null);

        assertThatThrownBy(() -> service.updateSettings(workspace, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("GEMINI");
    }

    @Test
    void keepsAnAlreadyChosenProviderWhosePluginWasRemoved() {
        workspace.setProviderSettings(
                new WorkspaceProviderSettings("GEMINI", Map.of(), null, Map.of(), null));

        service.updateSettings(
                workspace, new UpdateProviderSettingsRequest("GEMINI", null, null, null, null));

        assertThat(workspace.getProviderSettings().llmProvider()).isEqualTo("GEMINI");
    }
}
