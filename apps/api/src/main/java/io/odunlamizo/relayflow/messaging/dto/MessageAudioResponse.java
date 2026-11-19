package io.odunlamizo.relayflow.messaging.dto;

// Docs: docs/codebase-components.md#messaging-dto-records

import io.odunlamizo.relayflow.messaging.domain.TranscriptionStatus;

/** stored is true when the audio can be played from the message's audio route. */
public record MessageAudioResponse(
        String mimeType,
        Integer durationSeconds,
        boolean stored,
        TranscriptionStatus transcriptionStatus) {}
