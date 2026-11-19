package io.odunlamizo.relayflow.plugins.localdiskstorage;

// Docs: docs/codebase-components.md#official-plugin-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.odunlamizo.relayflow.plugin.ProviderException;
import io.odunlamizo.relayflow.plugin.StorageRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalDiskStorageProviderTest {

    @TempDir private Path directory;

    @Test
    void storedContentLoadsBack() {
        LocalDiskStorageProvider provider = provider();
        byte[] content = "voice".getBytes(StandardCharsets.UTF_8);

        String key =
                provider.store(
                        new StorageRequest(
                                "3f2b1c4e-0000-4000-8000-000000000001", "audio/ogg", content));

        assertThat(key).startsWith("3f2b1c4e-0000-4000-8000-000000000001/");
        assertThat(provider.load(key)).isEqualTo(content);
        assertThat(directory.resolve(key)).exists();
    }

    @Test
    void anUnsafeWorkspaceIdIsStoredInTheSharedFolder() {
        String key = provider().store(new StorageRequest("../etc", "audio/ogg", new byte[0]));

        assertThat(key).startsWith("shared/");
    }

    @Test
    void aKeyOutsideTheDirectoryIsRejected() {
        assertThatThrownBy(() -> provider().load("../outside"))
                .isInstanceOf(ProviderException.class);
    }

    private LocalDiskStorageProvider provider() {
        LocalDiskStorageProvider provider = new LocalDiskStorageProvider();
        provider.initialize(
                name ->
                        "RELAYFLOW_STORAGE_DIRECTORY".equals(name)
                                ? Optional.of(directory.toString())
                                : Optional.empty());

        return provider;
    }
}
