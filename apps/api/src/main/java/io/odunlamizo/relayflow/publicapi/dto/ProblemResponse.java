package io.odunlamizo.relayflow.publicapi.dto;

/** RFC 9457 problem details, plus a stable {@code code} for clients to branch on. */
public record ProblemResponse(
        String type, String title, int status, String detail, String instance, String code) {}
