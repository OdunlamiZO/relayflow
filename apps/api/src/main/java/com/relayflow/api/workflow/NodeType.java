package com.relayflow.api.workflow;

import java.util.Arrays;
import java.util.Optional;

/**
 * All node types recognised by the workflow engine and validator.
 *
 * <p>The {@link #getValue()} string is the type identifier stored in the graph JSON and returned by
 * each {@link com.relayflow.api.workflow.engine.NodeExecutor#nodeType()} implementation.
 */
public enum NodeType {
    TRIGGER("trigger"),
    SEND_MESSAGE("sendMessage"),
    CONDITION("condition"),
    HTTP_REQUEST("httpRequest"),
    SET_VARIABLE("setVariable"),
    END_CONVERSATION("endConversation"),
    WAIT_FOR_REPLY("waitForReply"),
    JUMP_TO("jumpTo");

    private final String value;

    NodeType(String value) {
        this.value = value;
    }

    /** The camelCase string used in the graph JSON (e.g. {@code "sendMessage"}). */
    public String getValue() {
        return value;
    }

    /** Returns the {@link NodeType} whose {@link #getValue()} matches {@code value}, if any. */
    public static Optional<NodeType> fromValue(String value) {
        return Arrays.stream(values()).filter(nodeType -> nodeType.value.equals(value)).findFirst();
    }
}
