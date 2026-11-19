package io.odunlamizo.relayflow.plugin;

// Docs: docs/codebase-components.md#transcriptionprovider

public interface TranscriptionProvider extends PluginProvider {

    String transcribe(TranscriptionRequest request);
}
