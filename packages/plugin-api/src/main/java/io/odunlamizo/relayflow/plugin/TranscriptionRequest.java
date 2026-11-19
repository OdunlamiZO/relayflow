package io.odunlamizo.relayflow.plugin;

// Docs: docs/codebase-components.md#transcriptionrequest

public record TranscriptionRequest(byte[] audio, String mimeType, String model) {}
