package com.relayflow.api.workflow.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mutable variable store for a single workflow run.
 *
 * <p>Variables are flat key → value pairs. Keys use dot-notation for built-in context variables
 * (e.g. {@code "contact.name"}, {@code "message.text"}) and plain names for user-defined variables.
 * Unlike respond.io, variables are fully reassignable — any Set Variable node can overwrite any
 * key.
 */
public class ExecutionContext {

    private final UUID runId;

    private final UUID conversationId;

    private final UUID workspaceId;

    private final Map<String, Object> variables;

    public ExecutionContext(
            UUID runId,
            UUID conversationId,
            UUID workspaceId,
            Map<String, Object> initialVariables) {
        this.runId = runId;
        this.conversationId = conversationId;
        this.workspaceId = workspaceId;
        this.variables = new LinkedHashMap<>(initialVariables);
    }

    public UUID getRunId() {
        return runId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    /** Returns an unmodifiable view of the current variable map. */
    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    /** Sets (or overwrites) a variable. Variables are always reassignable. */
    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    /** Resolves {@code {{placeholder}}} patterns in a string against the current variable map. */
    public String interpolate(String template) {
        return VariableInterpolator.interpolate(template, variables);
    }

    /** Returns a snapshot copy of all variables (used for step logging). */
    public Map<String, Object> snapshot() {
        return new LinkedHashMap<>(variables);
    }
}
