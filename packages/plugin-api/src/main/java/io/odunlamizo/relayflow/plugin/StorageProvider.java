package io.odunlamizo.relayflow.plugin;

// Docs: docs/codebase-components.md#storageprovider

public interface StorageProvider extends PluginProvider {

    String store(StorageRequest request);

    byte[] load(String key);
}
