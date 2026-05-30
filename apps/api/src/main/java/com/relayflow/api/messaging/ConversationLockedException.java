package com.relayflow.api.messaging;

/**
 * Thrown when an agent attempts to send a message on a conversation that is currently owned by an
 * active workflow run.
 */
public class ConversationLockedException extends RuntimeException {

    public ConversationLockedException(String message) {
        super(message);
    }
}
