package io.odunlamizo.relayflow.plugin;

// Docs: docs/codebase-components.md#chatmodelprovider

public interface ChatModelProvider extends PluginProvider {

    String complete(ChatRequest request);
}
