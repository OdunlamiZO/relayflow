package io.odunlamizo.relayflow.workflow;

// Docs: docs/codebase-components.md#workflowvalidationexception

public class WorkflowValidationException extends RuntimeException {

    public WorkflowValidationException(String message) {
        super(message);
    }
}
