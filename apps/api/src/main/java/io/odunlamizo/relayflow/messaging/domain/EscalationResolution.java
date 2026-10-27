package io.odunlamizo.relayflow.messaging.domain;

/** A human reply always clears an escalation, whatever its resolution. */
public enum EscalationResolution {
    HUMAN_REPLY,

    /** The AI agent's next successful LLM call also clears it. */
    AI_RECOVERY
}
