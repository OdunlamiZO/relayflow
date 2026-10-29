package io.odunlamizo.relayflow.messaging;

import io.odunlamizo.relayflow.common.ResourceNotFoundException;

public class ConversationNotFoundException extends ResourceNotFoundException {

    public ConversationNotFoundException() {
        super("Conversation not found");
    }
}
