package io.odunlamizo.relayflow.audio;

// Docs: docs/codebase-components.md#inboundaudio-and-inboundaudioreceivedevent

import java.util.UUID;

/** opensConversation is true when the message opened or reopened its conversation. */
public record InboundAudioReceivedEvent(
        UUID messageId, String mediaReference, boolean opensConversation) {}
