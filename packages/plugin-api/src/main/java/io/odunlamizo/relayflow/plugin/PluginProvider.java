package io.odunlamizo.relayflow.plugin;

// Docs: docs/codebase-components.md#pluginprovider

import java.util.Optional;

public interface PluginProvider {

    String id();

    String displayName();

    default void initialize(ProviderSettings settings) {}

    default Optional<String> configurationProblem() {
        return Optional.empty();
    }
}
