package com.relayflow.api.workflow.engine;

import java.util.Map;

public record GraphNode(String id, String type, Map<String, Object> data) {}
