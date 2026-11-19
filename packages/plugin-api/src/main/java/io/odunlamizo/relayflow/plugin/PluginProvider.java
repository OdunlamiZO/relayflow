package io.odunlamizo.relayflow.plugin;

// Docs: docs/codebase-components.md#pluginprovider

public interface PluginProvider {

    String id();

    String displayName();

    default void initialize(ProviderSettings settings) {}
}
