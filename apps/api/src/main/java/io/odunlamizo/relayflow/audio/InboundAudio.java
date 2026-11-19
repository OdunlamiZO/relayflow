package io.odunlamizo.relayflow.audio;

// Docs: docs/codebase-components.md#inboundaudio-and-inboundaudioreceivedevent

/** mediaReference is the channel's ID for the file, such as a Telegram file_id. */
public record InboundAudio(String mediaReference, String mimeType, Integer durationSeconds) {}
