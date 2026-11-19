package io.odunlamizo.relayflow.plugins.localdiskstorage;

// Docs: docs/codebase-components.md#localdiskstorageprovider

import io.odunlamizo.relayflow.plugin.ProviderException;
import io.odunlamizo.relayflow.plugin.ProviderSettings;
import io.odunlamizo.relayflow.plugin.StorageProvider;
import io.odunlamizo.relayflow.plugin.StorageRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

public class LocalDiskStorageProvider implements StorageProvider {

    private static final Pattern SAFE_FOLDER_NAME = Pattern.compile("[A-Za-z0-9-]+");

    private Path rootDirectory = Path.of("data", "storage").toAbsolutePath().normalize();

    @Override
    public String id() {
        return "LOCAL_DISK";
    }

    @Override
    public String displayName() {
        return "Local disk";
    }

    @Override
    public void initialize(ProviderSettings settings) {
        settings.get("RELAYFLOW_STORAGE_DIRECTORY")
                .filter(directory -> !directory.isBlank())
                .ifPresent(
                        directory ->
                                rootDirectory =
                                        Path.of(directory.strip()).toAbsolutePath().normalize());
    }

    @Override
    public String store(StorageRequest request) {
        String folder =
                request.workspaceId() != null
                                && SAFE_FOLDER_NAME.matcher(request.workspaceId()).matches()
                        ? request.workspaceId()
                        : "shared";
        String key = folder + "/" + UUID.randomUUID();
        Path file = resolve(key);

        try {
            Files.createDirectories(file.getParent());
            Files.write(file, request.content());
        } catch (IOException exception) {
            throw new ProviderException("Could not write " + file, exception);
        }

        return key;
    }

    @Override
    public byte[] load(String key) {
        Path file = resolve(key);

        try {
            return Files.readAllBytes(file);
        } catch (IOException exception) {
            throw new ProviderException("Could not read " + file, exception);
        }
    }

    private Path resolve(String key) {
        Path file = rootDirectory.resolve(key).normalize();

        if (!file.startsWith(rootDirectory)) {
            throw new ProviderException("Storage key points outside the storage directory");
        }

        return file;
    }
}
