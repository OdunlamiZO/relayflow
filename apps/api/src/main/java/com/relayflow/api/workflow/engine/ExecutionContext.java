package com.relayflow.api.workflow.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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

    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    public String interpolate(String template) {
        return VariableInterpolator.interpolate(template, variables);
    }

    public Map<String, Object> snapshot() {
        return new LinkedHashMap<>(variables);
    }
}
