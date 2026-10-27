package io.odunlamizo.relayflow.common.dto;

import java.time.Instant;

public record ErrorResponse(String message, Instant timestamp) {}
