package com.relayflow.api.messaging;

public class ConversationLockedException extends RuntimeException {

    public ConversationLockedException(String message) {
        super(message);
    }
}
