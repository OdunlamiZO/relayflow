package io.odunlamizo.relayflow.plugin;

// Docs: docs/codebase-components.md#storagerequest

public record StorageRequest(String workspaceId, String contentType, byte[] content) {}
