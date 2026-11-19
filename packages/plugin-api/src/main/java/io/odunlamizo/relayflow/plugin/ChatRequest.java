package io.odunlamizo.relayflow.plugin;

// Docs: docs/codebase-components.md#chatrequest-and-chatmessage

import java.util.List;

public record ChatRequest(String systemPrompt, List<ChatMessage> messages, String model) {}
