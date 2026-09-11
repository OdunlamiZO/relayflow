package com.relayflow.api.common.dto;

import java.time.Instant;

public record ErrorResponse(String message, Instant timestamp) {}
