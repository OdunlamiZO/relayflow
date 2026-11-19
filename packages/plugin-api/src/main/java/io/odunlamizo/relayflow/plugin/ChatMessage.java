package io.odunlamizo.relayflow.plugin;

// Docs: docs/codebase-components.md#chatrequest-and-chatmessage

public record ChatMessage(String role, String content) {}
