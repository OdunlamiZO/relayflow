package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.agent.domain.ExtractionField;
import io.odunlamizo.relayflow.plugin.ChatMessage;
import io.odunlamizo.relayflow.plugin.ChatModelProvider;
import io.odunlamizo.relayflow.plugin.ChatRequest;
import io.odunlamizo.relayflow.plugin.ProviderException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LlmClientTest {

    private final ChatModelProvider chatModelProvider = mock(ChatModelProvider.class);

    private final LlmPlatformConfigService configService = mock(LlmPlatformConfigService.class);

    private final AtomicReference<ChatRequest> sentRequest = new AtomicReference<>();

    private final LlmClient client =
            new LlmClient(chatModelProvider, null, configService, new ObjectMapper());

    @BeforeEach
    void setUp() {
        when(chatModelProvider.id()).thenReturn("OPENAI_COMPATIBLE");
        when(chatModelProvider.complete(any()))
                .thenAnswer(
                        invocation -> {
                            sentRequest.set(invocation.getArgument(0));

                            return "{\"reply\":\"Hello\",\"extractedData\":{\"amount\":\"500\"}}";
                        });
    }

    @Test
    void addsTheFormatInstructionAndParsesTheReply() {
        AgentLlmResponse response =
                client.complete(
                        new AgentLlmRequest(
                                "Be helpful",
                                List.of(LlmMessage.user("Hi")),
                                "llama-3.3-70b",
                                List.of(new ExtractionField("amount", "Loan amount", null))));

        assertThat(response.failed()).isFalse();
        assertThat(response.reply()).isEqualTo("Hello");
        assertThat(response.extractedData()).containsEntry("amount", "500");
        assertThat(sentRequest.get().systemPrompt()).startsWith("Be helpful").contains("amount");
        assertThat(sentRequest.get().messages()).containsExactly(new ChatMessage("user", "Hi"));
        assertThat(sentRequest.get().model()).isEqualTo("llama-3.3-70b");
    }

    @Test
    void readsTheModelFromRedisWhenTheAgentHasNone() {
        when(configService.getConfigValue("platform:llm:openai-compatible:model", null))
                .thenReturn("qwen-2.5");

        client.complete(new AgentLlmRequest("", List.of(), null, List.of()));

        assertThat(sentRequest.get().model()).isEqualTo("qwen-2.5");
    }

    @Test
    void theWorkspaceModelWinsOverRedis() {
        when(configService.getConfigValue("platform:llm:openai-compatible:model", null))
                .thenReturn("qwen-2.5");
        LlmClient withWorkspaceModel =
                new LlmClient(chatModelProvider, "llama-3.1-8b", configService, new ObjectMapper());

        withWorkspaceModel.complete(new AgentLlmRequest("", List.of(), null, List.of()));

        assertThat(sentRequest.get().model()).isEqualTo("llama-3.1-8b");
    }

    @Test
    void aProviderErrorIsAFailedResponse() {
        when(chatModelProvider.complete(any())).thenThrow(new ProviderException("down"));

        assertThat(client.complete(new AgentLlmRequest("", List.of(), "m", List.of())).failed())
                .isTrue();
    }

    @Test
    void withoutAProviderEveryCallFails() {
        LlmClient withoutProvider = new LlmClient(null, null, configService, new ObjectMapper());

        assertThat(withoutProvider.provider()).isNull();
        assertThat(
                        withoutProvider
                                .complete(new AgentLlmRequest("", List.of(), null, List.of()))
                                .failed())
                .isTrue();
    }

    @Test
    void modelKeysUseTheLowercaseHyphenatedProviderId() {
        assertThat(LlmClient.modelKey("ANTHROPIC")).isEqualTo("platform:llm:anthropic:model");
        assertThat(LlmClient.modelKey("OPENAI_COMPATIBLE"))
                .isEqualTo("platform:llm:openai-compatible:model");
    }
}
