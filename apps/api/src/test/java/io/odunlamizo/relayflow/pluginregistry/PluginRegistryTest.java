package io.odunlamizo.relayflow.pluginregistry;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.odunlamizo.relayflow.plugin.ChatModelProvider;
import io.odunlamizo.relayflow.plugin.ChatRequest;
import io.odunlamizo.relayflow.plugin.ProviderSettings;
import io.odunlamizo.relayflow.plugin.StorageProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class PluginRegistryTest {

    @TempDir private Path directory;

    @Test
    void loadsProvidersFromJarsInTheDirectoryAndPassesThemTheirSettings() throws IOException {
        writePluginJar(
                directory.resolve("nested/fake.jar"),
                ChatModelProvider.class,
                FakeChatModelProvider.class.getName());

        PluginRegistry registry =
                new PluginRegistry(
                        directory.toString(), new MockEnvironment().withProperty("FAKE_KEY", "k"));

        assertThat(registry.chatModelProviders()).hasSize(1);
        FakeChatModelProvider provider =
                (FakeChatModelProvider) registry.findChatModelProvider("FAKE").orElseThrow();
        assertThat(provider.key).isEqualTo("k");
        assertThat(registry.findChatModelProvider("OTHER")).isEmpty();
        assertThat(registry.storageProviders()).isEmpty();
    }

    @Test
    void aMissingDirectoryLoadsNothing() {
        PluginRegistry registry =
                new PluginRegistry(directory.resolve("absent").toString(), new MockEnvironment());

        assertThat(registry.chatModelProviders()).isEmpty();
        assertThat(registry.transcriptionProviders()).isEmpty();
    }

    @Test
    void aProviderThatFailsToLoadIsSkipped() throws IOException {
        writePluginJar(
                directory.resolve("broken.jar"),
                StorageProvider.class,
                "io.odunlamizo.relayflow.missing.MissingStorageProvider");

        PluginRegistry registry = new PluginRegistry(directory.toString(), new MockEnvironment());

        assertThat(registry.storageProviders()).isEmpty();
    }

    @Test
    void twoProvidersWithTheSameIdStopStartup() throws IOException {
        writePluginJar(
                directory.resolve("first.jar"),
                ChatModelProvider.class,
                FakeChatModelProvider.class.getName());
        writePluginJar(
                directory.resolve("second.jar"),
                ChatModelProvider.class,
                SameIdChatModelProvider.class.getName());

        assertThatThrownBy(() -> new PluginRegistry(directory.toString(), new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAKE");
    }

    private static void writePluginJar(Path jar, Class<?> type, String implementation)
            throws IOException {
        Files.createDirectories(jar.getParent());

        try (OutputStream file = Files.newOutputStream(jar);
                JarOutputStream output = new JarOutputStream(file)) {
            output.putNextEntry(new JarEntry("META-INF/services/" + type.getName()));
            output.write(implementation.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }

    public static class FakeChatModelProvider implements ChatModelProvider {

        private String key;

        @Override
        public String id() {
            return "FAKE";
        }

        @Override
        public String displayName() {
            return "Fake";
        }

        @Override
        public void initialize(ProviderSettings settings) {
            key = settings.get("FAKE_KEY").orElse(null);
        }

        @Override
        public String complete(ChatRequest request) {
            return "{}";
        }
    }

    public static class SameIdChatModelProvider extends FakeChatModelProvider {}
}
