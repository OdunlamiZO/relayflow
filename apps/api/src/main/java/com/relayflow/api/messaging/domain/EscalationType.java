package com.relayflow.api.messaging.domain;

public enum EscalationType {
    KEYWORD_MATCHED(EscalationResolution.HUMAN_REPLY),
    AI_REQUESTED(EscalationResolution.HUMAN_REPLY),
    LLM_FAILED(EscalationResolution.AI_RECOVERY),
    INTERNAL_ERROR(EscalationResolution.AI_RECOVERY);

    private final EscalationResolution resolution;

    EscalationType(EscalationResolution resolution) {
        this.resolution = resolution;
    }

    public EscalationResolution getResolution() {
        return resolution;
    }
}
