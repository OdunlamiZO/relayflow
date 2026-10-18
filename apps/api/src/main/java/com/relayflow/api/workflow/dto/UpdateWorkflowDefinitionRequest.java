package com.relayflow.api.workflow.dto;

import java.util.Map;

/** Only non-null fields are applied. */
public record UpdateWorkflowDefinitionRequest(
        String name, Boolean enabled, Map<String, Object> draftGraph) {}
