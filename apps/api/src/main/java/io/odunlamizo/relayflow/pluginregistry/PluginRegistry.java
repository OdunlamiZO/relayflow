package io.odunlamizo.relayflow.pluginregistry;

// Docs: docs/codebase-components.md#pluginregistry

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.plugin.ChatModelProvider;
import io.odunlamizo.relayflow.plugin.PluginProvider;
import io.odunlamizo.relayflow.plugin.ProviderSettings;
import io.odunlamizo.relayflow.plugin.StorageProvider;
import io.odunlamizo.relayflow.plugin.TranscriptionProvider;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class PluginRegistry {

    private static final Logger log = LoggerFactory.getLogger(PluginRegistry.class);

    private final Map<String, ChatModelProvider> chatModelProviders;

    private final Map<String, TranscriptionProvider> transcriptionProviders;

    private final Map<String, StorageProvider> storageProviders;

    public PluginRegistry(
            @Value("${relayflow.plugins.directory}") String directory, Environment environment) {
        ClassLoader classLoader = pluginClassLoader(Path.of(directory));
        ProviderSettings settings = new EnvironmentProviderSettings(environment);

        this.chatModelProviders = load(ChatModelProvider.class, classLoader, settings);
        this.transcriptionProviders = load(TranscriptionProvider.class, classLoader, settings);
        this.storageProviders = load(StorageProvider.class, classLoader, settings);
    }

    public List<ChatModelProvider> chatModelProviders() {
        return sorted(chatModelProviders);
    }

    public List<TranscriptionProvider> transcriptionProviders() {
        return sorted(transcriptionProviders);
    }

    public List<StorageProvider> storageProviders() {
        return sorted(storageProviders);
    }

    public Optional<ChatModelProvider> findChatModelProvider(String id) {
        return Optional.ofNullable(id).map(chatModelProviders::get);
    }

    public Optional<TranscriptionProvider> findTranscriptionProvider(String id) {
        return Optional.ofNullable(id).map(transcriptionProviders::get);
    }

    public Optional<StorageProvider> findStorageProvider(String id) {
        return Optional.ofNullable(id).map(storageProviders::get);
    }

    private static ClassLoader pluginClassLoader(Path directory) {
        ClassLoader parent = PluginRegistry.class.getClassLoader();

        if (!Files.isDirectory(directory)) {
            log.info(
                    "Plugin directory not found, loading plugins from the classpath only {}",
                    kv("directory", directory.toAbsolutePath()));

            return parent;
        }

        try (Stream<Path> files = Files.walk(directory)) {
            URL[] jars =
                    files.filter(file -> file.toString().endsWith(".jar"))
                            .sorted()
                            .map(PluginRegistry::toUrl)
                            .toArray(URL[]::new);

            log.info(
                    "Plugin JARs found {} {}",
                    kv("directory", directory.toAbsolutePath()),
                    kv("count", jars.length));

            return new URLClassLoader(jars, parent);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read plugin directory " + directory, exception);
        }
    }

    private static URL toUrl(Path file) {
        try {
            return file.toUri().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalStateException("Invalid plugin path " + file, exception);
        }
    }

    private static <T extends PluginProvider> Map<String, T> load(
            Class<T> type, ClassLoader classLoader, ProviderSettings settings) {
        Map<String, T> providers = new LinkedHashMap<>();
        Iterator<T> candidates = ServiceLoader.load(type, classLoader).iterator();

        while (true) {
            T provider;

            try {
                if (!candidates.hasNext()) {
                    break;
                }

                provider = candidates.next();
                provider.initialize(settings);
            } catch (RuntimeException | ServiceConfigurationError exception) {
                log.error(
                        "Plugin provider failed to start and was skipped {}",
                        kv("type", type.getSimpleName()),
                        exception);

                continue;
            }

            T existing = providers.putIfAbsent(provider.id(), provider);

            if (existing != null) {
                throw new IllegalStateException(
                        "Two "
                                + type.getSimpleName()
                                + " plugins use the ID "
                                + provider.id()
                                + ": "
                                + existing.getClass().getName()
                                + " and "
                                + provider.getClass().getName());
            }

            log.info(
                    "Plugin provider loaded {} {} {}",
                    kv("type", type.getSimpleName()),
                    kv("id", provider.id()),
                    kv("class", provider.getClass().getName()));
        }

        return providers;
    }

    private static <T extends PluginProvider> List<T> sorted(Map<String, T> providers) {
        return providers.values().stream()
                .sorted(Comparator.comparing(PluginProvider::displayName))
                .toList();
    }
}
