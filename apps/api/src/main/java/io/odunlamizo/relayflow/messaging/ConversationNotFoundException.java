package io.odunlamizo.relayflow.messaging;

// Docs: docs/codebase-components.md#conversationnotfoundexception

import io.odunlamizo.relayflow.common.ResourceNotFoundException;

public class ConversationNotFoundException extends ResourceNotFoundException {

    public ConversationNotFoundException() {
        super("Conversation not found");
    }
}
