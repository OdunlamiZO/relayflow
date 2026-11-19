package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.plugin.ChatModelProvider;
import io.odunlamizo.relayflow.pluginregistry.PluginRegistry;
import java.util.List;
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
        when(configService.getActiveProvider()).thenReturn("ANTHROPIC");

        assertThat(factory.getClient("OPENAI_COMPATIBLE").provider())
                .isEqualTo("OPENAI_COMPATIBLE");
    }

    @Test
    void usesThePlatformProviderWithoutAnOverride() {
        when(configService.getActiveProvider()).thenReturn("OPENAI_COMPATIBLE");

        assertThat(factory.getActiveClient().provider()).isEqualTo("OPENAI_COMPATIBLE");
    }

    @Test
    void anOverrideThatIsNotInstalledFallsBackToThePlatformProvider() {
        when(configService.getActiveProvider()).thenReturn("ANTHROPIC");

        assertThat(factory.getClient("GEMINI").provider()).isEqualTo("ANTHROPIC");
    }

    @Test
    void aPlatformProviderThatIsNotInstalledFallsBackToTheFirstInstalledOne() {
        when(configService.getActiveProvider()).thenReturn("GEMINI");

        assertThat(factory.getActiveClient().provider()).isEqualTo("ANTHROPIC");
    }

    @Test
    void withNoPluginsInstalledTheClientHasNoProvider() {
        when(configService.getActiveProvider()).thenReturn("ANTHROPIC");
        when(pluginRegistry.findChatModelProvider("ANTHROPIC")).thenReturn(Optional.empty());
        when(pluginRegistry.chatModelProviders()).thenReturn(List.of());

        assertThat(factory.getActiveClient().provider()).isNull();
    }

    private static ChatModelProvider provider(String id) {
        ChatModelProvider provider = mock(ChatModelProvider.class);
        when(provider.id()).thenReturn(id);

        return provider;
    }
}
