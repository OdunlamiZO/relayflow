package com.relayflow.api.subscription.domain;

/** The resource types that are capped per plan. */
public enum LimitType {
    CHANNEL_ACCOUNTS("channel connections"),
    WORKFLOWS("workflows"),
    MEMBERS_PER_WORKSPACE("workspace members");

    private final String label;

    LimitType(String label) {
        this.label = label;
    }

    /** Human-readable label used in error messages. */
    public String label() {

        return label;
    }
}
