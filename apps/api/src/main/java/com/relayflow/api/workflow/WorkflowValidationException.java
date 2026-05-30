package com.relayflow.api.workflow;

/** Thrown when a workflow graph fails structural validation before being enabled. */
public class WorkflowValidationException extends RuntimeException {

    public WorkflowValidationException(String message) {
        super(message);
    }
}
