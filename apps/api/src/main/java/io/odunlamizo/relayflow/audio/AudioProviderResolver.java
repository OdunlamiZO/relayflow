package io.odunlamizo.relayflow.audio;

// Docs: docs/codebase-components.md#audioproviderresolver

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.plugin.PluginProvider;
import io.odunlamizo.relayflow.plugin.StorageProvider;
import io.odunlamizo.relayflow.plugin.TranscriptionProvider;
import io.odunlamizo.relayflow.pluginregistry.PluginRegistry;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceProviderSettings;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AudioProviderResolver {

    private static final Logger log = LoggerFactory.getLogger(AudioProviderResolver.class);

    private static final String STORAGE_PROVIDER_KEY = "platform:storage:provider";

    private static final String TRANSCRIPTION_PROVIDER_KEY = "platform:transcription:provider";

    private final PluginRegistry pluginRegistry;

    private final StringRedisTemplate redis;

    public AudioProviderResolver(PluginRegistry pluginRegistry, StringRedisTemplate redis) {
        this.pluginRegistry = pluginRegistry;
        this.redis = redis;
    }

    public Optional<StorageProvider> activeStorageProvider(WorkspaceProviderSettings settings) {
        return workspaceChoice(settings.storageProvider(), pluginRegistry::findStorageProvider)
                .or(this::instanceStorageProvider);
    }

    public Optional<TranscriptionProvider> activeTranscriptionProvider(
            WorkspaceProviderSettings settings) {
        return workspaceChoice(
                        settings.transcriptionProvider(), pluginRegistry::findTranscriptionProvider)
                .or(this::instanceTranscriptionProvider);
    }

    public Optional<StorageProvider> instanceStorageProvider() {
        return resolve(STORAGE_PROVIDER_KEY, pluginRegistry::findStorageProvider);
    }

    public Optional<TranscriptionProvider> instanceTranscriptionProvider() {
        return resolve(TRANSCRIPTION_PROVIDER_KEY, pluginRegistry::findTranscriptionProvider);
    }

    public Optional<StorageProvider> storageProvider(String id) {
        return pluginRegistry.findStorageProvider(id);
    }

    public String transcriptionModel(WorkspaceProviderSettings settings, String providerId) {
        return settings.transcriptionModel(providerId)
                .filter(model -> !model.isBlank())
                .orElseGet(() -> instanceTranscriptionModel(providerId));
    }

    public String instanceTranscriptionModel(String providerId) {
        return read("platform:transcription:"
                        + providerId.toLowerCase().replace('_', '-')
                        + ":model")
                .orElse(null);
    }

    private <T extends PluginProvider> Optional<T> workspaceChoice(
            String providerId, Function<String, Optional<T>> finder) {
        if (providerId == null) {
            return Optional.empty();
        }

        Optional<T> provider = finder.apply(providerId);

        if (provider.isEmpty()) {
            log.warn(
                    "Workspace's plugin provider isn't installed, using the instance's {}",
                    kv("provider", providerId));
        }

        return provider;
    }

    private <T extends PluginProvider> Optional<T> resolve(
            String key, Function<String, Optional<T>> finder) {
        Optional<String> configured = read(key).map(value -> value.toUpperCase().replace('-', '_'));

        if (configured.isEmpty()) {
            return Optional.empty();
        }

        Optional<T> provider = finder.apply(configured.get());

        if (provider.isEmpty()) {
            log.warn(
                    "Configured plugin provider isn't installed {} {}",
                    kv("key", key),
                    kv("provider", configured.get()));
        }

        return provider;
    }

    private Optional<String> read(String key) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(key))
                    .map(String::strip)
                    .filter(value -> !value.isEmpty());
        } catch (Exception exception) {
            log.warn(
                    "Redis read failed {} {}", kv("key", key), kv("error", exception.getMessage()));

            return Optional.empty();
        }
    }
}
