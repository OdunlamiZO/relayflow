package com.relayflow.api.workflow.domain;

public enum WorkflowRunStatus {
    RUNNING,
    /** Paused at a "Wait for Reply" node; execution resumes when the contact replies. */
    WAITING,
    COMPLETED,
    FAILED
}
