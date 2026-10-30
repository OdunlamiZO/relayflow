package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class LlmPlatformConfigServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);

    private final LlmPlatformConfigService service = new LlmPlatformConfigService(redis);

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPENAI", "GROQ", "ollama", "OPENROUTER"})
    void formerOpenAiCompatibleProvidersMeanCustom(String provider) {
        when(values.get("platform:llm:provider")).thenReturn(provider);

        assertThat(service.getActiveProvider()).isEqualTo(LlmProvider.OPENAI_COMPATIBLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPENAI_COMPATIBLE", "openai-compatible", " anthropic "})
    void readsCurrentProvidersWithHyphensOrUnderscores(String provider) {
        when(values.get("platform:llm:provider")).thenReturn(provider);

        assertThat(service.getActiveProvider())
                .isEqualTo(
                        provider.contains("anthropic")
                                ? LlmProvider.ANTHROPIC
                                : LlmProvider.OPENAI_COMPATIBLE);
    }

    @Test
    void defaultsToAnthropicWhenUnsetOrUnknown() {
        when(values.get("platform:llm:provider")).thenReturn(null, "GEMINI");

        assertThat(service.getActiveProvider()).isEqualTo(LlmProvider.ANTHROPIC);
        assertThat(service.getActiveProvider()).isEqualTo(LlmProvider.ANTHROPIC);
    }
}
