package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.plugin.ChatModelProvider;
import io.odunlamizo.relayflow.pluginregistry.PluginRegistry;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceProviderSettings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LlmClientFactoryTest {

    private final ChatModelProvider anthropic = provider("ANTHROPIC");

    private final ChatModelProvider openAiCompatible = provider("OPENAI_COMPATIBLE");

    private final PluginRegistry pluginRegistry = mock(PluginRegistry.class);

    private final LlmPlatformConfigService configService = mock(LlmPlatformConfigService.class);

    private final LlmClientFactory factory =
            new LlmClientFactory(pluginRegistry, configService, new ObjectMapper());

    @BeforeEach
    void setUp() {
        when(pluginRegistry.findChatModelProvider("ANTHROPIC")).thenReturn(Optional.of(anthropic));
        when(pluginRegistry.findChatModelProvider("OPENAI_COMPATIBLE"))
                .thenReturn(Optional.of(openAiCompatible));
        when(pluginRegistry.findChatModelProvider("GEMINI")).thenReturn(Optional.empty());
        when(pluginRegistry.chatModelProviders()).thenReturn(List.of(anthropic, openAiCompatible));
    }

    @Test
    void anInstalledOverrideWinsOverThePlatformProvider() {
        when(configService.getActiveProvider()).thenReturn(Optional.of("ANTHROPIC"));

        assertThat(
                        factory.getClient(WorkspaceProviderSettings.NONE, "OPENAI_COMPATIBLE")
                                .provider())
                .isEqualTo("OPENAI_COMPATIBLE");
    }

    @Test
    void usesThePlatformProviderWithoutAnOverride() {
        when(configService.getActiveProvider()).thenReturn(Optional.of("OPENAI_COMPATIBLE"));

        assertThat(factory.getClient(WorkspaceProviderSettings.NONE, null).provider())
                .isEqualTo("OPENAI_COMPATIBLE");
    }

    @Test
    void theWorkspaceProviderWinsOverThePlatformProvider() {
        when(configService.getActiveProvider()).thenReturn(Optional.of("ANTHROPIC"));

        assertThat(factory.getClient(workspaceChoosing("OPENAI_COMPATIBLE"), null).provider())
                .isEqualTo("OPENAI_COMPATIBLE");
    }

    @Test
    void anAgentOverrideWinsOverTheWorkspaceProvider() {
        assertThat(
                        factory.getClient(workspaceChoosing("OPENAI_COMPATIBLE"), "ANTHROPIC")
                                .provider())
                .isEqualTo("ANTHROPIC");
    }

    @Test
    void aWorkspaceProviderThatIsNotInstalledFallsBackToThePlatformProvider() {
        when(configService.getActiveProvider()).thenReturn(Optional.of("OPENAI_COMPATIBLE"));

        assertThat(factory.getClient(workspaceChoosing("GEMINI"), null).provider())
                .isEqualTo("OPENAI_COMPATIBLE");
    }

    @Test
    void usesTheWorkspaceModelForTheChosenProvider() {
        when(configService.getActiveProvider()).thenReturn(Optional.empty());
        WorkspaceProviderSettings settings =
                new WorkspaceProviderSettings(
                        "OPENAI_COMPATIBLE",
                        Map.of("OPENAI_COMPATIBLE", "llama-3.1-8b", "ANTHROPIC", "claude-opus"),
                        null,
                        Map.of(),
                        null);

        factory.getClient(settings, null)
                .complete(new AgentLlmRequest("", List.of(), null, List.of()));

        verify(openAiCompatible)
                .complete(argThat(request -> "llama-3.1-8b".equals(request.model())));
    }

    @Test
    void anOverrideThatIsNotInstalledFallsBackToThePlatformProvider() {
        when(configService.getActiveProvider()).thenReturn(Optional.of("ANTHROPIC"));

        assertThat(factory.getClient(WorkspaceProviderSettings.NONE, "GEMINI").provider())
                .isEqualTo("ANTHROPIC");
    }

    @Test
    void aPlatformProviderThatIsNotInstalledMeansNoProvider() {
        when(configService.getActiveProvider()).thenReturn(Optional.of("GEMINI"));

        assertThat(factory.getClient(WorkspaceProviderSettings.NONE, null).provider()).isNull();
    }

    @Test
    void withNothingChosenThereIsNoProviderEvenWithPluginsInstalled() {
        when(configService.getActiveProvider()).thenReturn(Optional.empty());

        assertThat(factory.getClient(WorkspaceProviderSettings.NONE, null).provider()).isNull();
    }

    @Test
    void withNoPluginsInstalledTheClientHasNoProvider() {
        when(configService.getActiveProvider()).thenReturn(Optional.of("ANTHROPIC"));
        when(pluginRegistry.findChatModelProvider("ANTHROPIC")).thenReturn(Optional.empty());

        assertThat(factory.getClient(WorkspaceProviderSettings.NONE, null).provider()).isNull();
    }

    private static WorkspaceProviderSettings workspaceChoosing(String providerId) {
        return new WorkspaceProviderSettings(providerId, Map.of(), null, Map.of(), null);
    }

    private static ChatModelProvider provider(String id) {
        ChatModelProvider provider = mock(ChatModelProvider.class);
        when(provider.id()).thenReturn(id);

        return provider;
    }
}
