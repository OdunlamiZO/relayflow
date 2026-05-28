package com.relayflow.api.messaging.dto;

import java.time.Instant;

public record ErrorResponse(String message, Instant timestamp) {}
