package io.odunlamizo.relayflow.pluginregistry;

// Docs: docs/codebase-components.md#environmentprovidersettings

import io.odunlamizo.relayflow.plugin.ProviderSettings;
import java.util.Optional;
import org.springframework.core.env.Environment;

public class EnvironmentProviderSettings implements ProviderSettings {

    private final Environment environment;

    public EnvironmentProviderSettings(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Optional<String> get(String name) {
        return Optional.ofNullable(environment.getProperty(name));
    }
}
