package com.relayflow.api.webhook.dto;

import java.util.Map;

/**
 * The {@code contact.updated} and {@code contact.deleted} payload, built by {@code
 * ContactSnapshotBuilder}.
 */
public record ContactSnapshot(Map<String, Object> contact) {}
