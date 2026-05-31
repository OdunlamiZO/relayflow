package com.relayflow.api.profile.dto;

/**
 * {@code password} is required for EMAIL-provider accounts; OAuth users may omit it (null is
 * allowed — the controller passes null and the service skips password validation).
 */
public record DeleteAccountRequest(String password) {}
