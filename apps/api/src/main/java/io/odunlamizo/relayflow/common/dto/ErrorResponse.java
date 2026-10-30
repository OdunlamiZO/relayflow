package io.odunlamizo.relayflow.common.dto;

// Docs: docs/codebase-components.md#errorresponse

import java.time.Instant;

public record ErrorResponse(String message, Instant timestamp) {}
