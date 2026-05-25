package com.relayflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RelayFlowApiApplicationSmokeTest {
    @Test
    void applicationClassExists() {
        assertThat(RelayFlowApiApplication.class).isNotNull();
    }
}
