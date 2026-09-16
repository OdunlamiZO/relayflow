package com.relayflow.api.agent.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class LlmClientFactoryTest {

    private final LlmClient anthropicClient = stubClient(LlmProvider.ANTHROPIC);

    private final LlmClient groqClient = stubClient(LlmProvider.GROQ);

    private final LlmPlatformConfigService configService = mock(LlmPlatformConfigService.class);

    private final LlmClientFactory factory =
            new LlmClientFactory(List.of(anthropicClient, groqClient), configService);

    @Test
    void getClientReturnsExplicitOverrideRegardlessOfPlatformDefault() {
        when(configService.getActiveProvider()).thenReturn(LlmProvider.ANTHROPIC);

        assertThat(factory.getClient(LlmProvider.GROQ)).isSameAs(groqClient);
    }

    @Test
    void getClientFallsBackToPlatformDefaultWhenOverrideIsNull() {
        when(configService.getActiveProvider()).thenReturn(LlmProvider.GROQ);

        assertThat(factory.getClient(null)).isSameAs(groqClient);
    }

    @Test
    void getActiveClientUsesPlatformDefault() {
        when(configService.getActiveProvider()).thenReturn(LlmProvider.ANTHROPIC);

        assertThat(factory.getActiveClient()).isSameAs(anthropicClient);
    }

    @Test
    void getClientFallsBackToAnthropicWhenOverrideHasNoRegisteredClient() {
        assertThat(factory.getClient(LlmProvider.OPENAI)).isSameAs(anthropicClient);
    }

    private static LlmClient stubClient(LlmProvider provider) {
        LlmClient client = mock(LlmClient.class);
        when(client.provider()).thenReturn(provider);

        return client;
    }
}
