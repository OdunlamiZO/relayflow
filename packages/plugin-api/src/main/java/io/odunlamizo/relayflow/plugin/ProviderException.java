package io.odunlamizo.relayflow.plugin;

// Docs: docs/codebase-components.md#providerexception

public class ProviderException extends RuntimeException {

    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
