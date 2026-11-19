package io.odunlamizo.relayflow.plugin;

// Docs: docs/codebase-components.md#providersettings

import java.util.Optional;

public interface ProviderSettings {

    Optional<String> get(String name);
}
