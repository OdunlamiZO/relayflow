package io.odunlamizo.relayflow.messaging;

// Docs: docs/codebase-components.md#conversationlockedexception

public class ConversationLockedException extends RuntimeException {

    public ConversationLockedException(String message) {
        super(message);
    }
}
