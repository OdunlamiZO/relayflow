package com.relayflow.api.agent.domain;

public enum AutonomyCeiling {
    /** Every response goes to a human draft regardless of confidence. */
    DRAFT_ONLY,

    /** High-confidence, non-escalated responses are sent directly to the contact. */
    AUTO_SEND
}
