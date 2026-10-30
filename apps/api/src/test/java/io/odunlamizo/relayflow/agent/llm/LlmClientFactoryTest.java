package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class LlmClientFactoryTest {

    private final LlmClient anthropicClient = stubClient(LlmProvider.ANTHROPIC);

    private final LlmClient openAiCompatibleClient = stubClient(LlmProvider.OPENAI_COMPATIBLE);

    private final LlmPlatformConfigService configService = mock(LlmPlatformConfigService.class);

    private final LlmClientFactory factory =
            new LlmClientFactory(List.of(anthropicClient, openAiCompatibleClient), configService);

    @Test
    void getClientReturnsExplicitOverrideRegardlessOfPlatformDefault() {
        when(configService.getActiveProvider()).thenReturn(LlmProvider.ANTHROPIC);

        assertThat(factory.getClient(LlmProvider.OPENAI_COMPATIBLE))
                .isSameAs(openAiCompatibleClient);
    }

    @Test
    void getClientFallsBackToPlatformDefaultWhenOverrideIsNull() {
        when(configService.getActiveProvider()).thenReturn(LlmProvider.OPENAI_COMPATIBLE);

        assertThat(factory.getClient(null)).isSameAs(openAiCompatibleClient);
    }

    @Test
    void getActiveClientUsesPlatformDefault() {
        when(configService.getActiveProvider()).thenReturn(LlmProvider.ANTHROPIC);

        assertThat(factory.getActiveClient()).isSameAs(anthropicClient);
    }

    @Test
    void getClientFallsBackToAnthropicWhenOverrideHasNoRegisteredClient() {
        LlmClientFactory anthropicOnly =
                new LlmClientFactory(List.of(anthropicClient), configService);

        assertThat(anthropicOnly.getClient(LlmProvider.OPENAI_COMPATIBLE))
                .isSameAs(anthropicClient);
    }

    private static LlmClient stubClient(LlmProvider provider) {
        LlmClient client = mock(LlmClient.class);
        when(client.provider()).thenReturn(provider);

        return client;
    }
}
