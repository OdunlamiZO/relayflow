package io.odunlamizo.relayflow.audio;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.odunlamizo.relayflow.plugin.StorageProvider;
import io.odunlamizo.relayflow.plugin.TranscriptionProvider;
import io.odunlamizo.relayflow.pluginregistry.PluginRegistry;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceProviderSettings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AudioProviderResolverTest {

    private final PluginRegistry pluginRegistry = mock(PluginRegistry.class);

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);

    private final TranscriptionProvider groq = transcription("GROQ");

    private final TranscriptionProvider openAiCompatible = transcription("OPENAI_COMPATIBLE");

    private final StorageProvider localDisk = mock(StorageProvider.class);

    private final AudioProviderResolver resolver = new AudioProviderResolver(pluginRegistry, redis);

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        when(pluginRegistry.transcriptionProviders()).thenReturn(List.of(groq, openAiCompatible));
        when(pluginRegistry.findTranscriptionProvider("GROQ")).thenReturn(Optional.of(groq));
        when(pluginRegistry.findTranscriptionProvider("OPENAI_COMPATIBLE"))
                .thenReturn(Optional.of(openAiCompatible));
        when(pluginRegistry.findTranscriptionProvider("WHISPER_LOCAL"))
                .thenReturn(Optional.empty());
        when(pluginRegistry.storageProviders()).thenReturn(List.of(localDisk));
    }

    @Test
    void theWorkspaceTranscriptionProviderWinsOverRedis() {
        when(values.get("platform:transcription:provider")).thenReturn("OPENAI_COMPATIBLE");

        assertThat(resolver.activeTranscriptionProvider(choosing("GROQ"))).contains(groq);
    }

    @Test
    void aWorkspaceProviderThatIsNotInstalledFallsBackToRedis() {
        when(values.get("platform:transcription:provider")).thenReturn("OPENAI_COMPATIBLE");

        assertThat(resolver.activeTranscriptionProvider(choosing("WHISPER_LOCAL")))
                .contains(openAiCompatible);
    }

    @Test
    void withNothingChosenThereIsNoTranscriptionProvider() {
        assertThat(resolver.activeTranscriptionProvider(WorkspaceProviderSettings.NONE)).isEmpty();
    }

    @Test
    void withNothingChosenThereIsNoStorageProviderEvenWithOneInstalled() {
        assertThat(resolver.activeStorageProvider(WorkspaceProviderSettings.NONE)).isEmpty();
    }

    @Test
    void theWorkspaceTranscriptionModelWinsOverRedis() {
        when(values.get("platform:transcription:groq:model")).thenReturn("whisper-large-v3");
        WorkspaceProviderSettings settings =
                new WorkspaceProviderSettings(
                        null, Map.of(), "GROQ", Map.of("GROQ", "whisper-large-v3-turbo"), null);

        assertThat(resolver.transcriptionModel(settings, "GROQ"))
                .isEqualTo("whisper-large-v3-turbo");
        assertThat(resolver.transcriptionModel(WorkspaceProviderSettings.NONE, "GROQ"))
                .isEqualTo("whisper-large-v3");
    }

    private static WorkspaceProviderSettings choosing(String transcriptionProvider) {
        return new WorkspaceProviderSettings(null, Map.of(), transcriptionProvider, Map.of(), null);
    }

    private static TranscriptionProvider transcription(String id) {
        TranscriptionProvider provider = mock(TranscriptionProvider.class);
        when(provider.id()).thenReturn(id);

        return provider;
    }
}
