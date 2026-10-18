package com.relayflow.api.workflow;

import java.util.Arrays;
import java.util.Optional;

public enum NodeType {
    TRIGGER("trigger"),
    SEND_MESSAGE("sendMessage"),
    CONDITION("condition"),
    HTTP_REQUEST("httpRequest"),
    SET_VARIABLE("setVariable"),
    SET_CONTACT_FIELD("setContactField"),
    SET_CONTACT_TAG("setContactTag"),
    END_CONVERSATION("endConversation"),
    WAIT_FOR_REPLY("waitForReply"),
    JUMP_TO("jumpTo");

    private final String value;

    NodeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<NodeType> fromValue(String value) {
        return Arrays.stream(values()).filter(nodeType -> nodeType.value.equals(value)).findFirst();
    }
}
