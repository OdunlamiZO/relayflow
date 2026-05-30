package com.relayflow.api.workflow.engine;

/** Thrown by a node executor when execution fails and the error branch should be followed. */
public class NodeExecutionException extends RuntimeException {

    public NodeExecutionException(String message) {
        super(message);
    }

    public NodeExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
