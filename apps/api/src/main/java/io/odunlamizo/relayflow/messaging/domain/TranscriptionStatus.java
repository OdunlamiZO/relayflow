package io.odunlamizo.relayflow.messaging.domain;

// Docs: docs/codebase-components.md#transcriptionstatus

public enum TranscriptionStatus {
    PENDING,

    TRANSCRIBED,

    FAILED,

    NO_PROVIDER
}
