package com.relayflow.api.workflow.dto;

import java.util.Map;

/** All fields are optional — only non-null values are applied to the workflow definition. */
public record UpdateWorkflowDefinitionRequest(
        String name, Boolean enabled, Map<String, Object> draftGraph) {}
