package io.odunlamizo.relayflow.messaging;

public class ConversationLockedException extends RuntimeException {

    public ConversationLockedException(String message) {
        super(message);
    }
}
