package com.relayflow.api.messaging.domain;

/** What clears an escalation. A human reply always clears one, whatever its resolution. */
public enum EscalationResolution {
    HUMAN_REPLY,

    /** Temporary: the AI agent's next successful LLM call also clears it. */
    AI_RECOVERY
}
