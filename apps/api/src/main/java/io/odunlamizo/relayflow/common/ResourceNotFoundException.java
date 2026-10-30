package io.odunlamizo.relayflow.common;

// Docs: docs/codebase-components.md#resourcenotfoundexception

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
