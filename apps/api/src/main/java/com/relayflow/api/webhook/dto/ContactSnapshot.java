package com.relayflow.api.webhook.dto;

import java.util.Map;

/**
 * A point-in-time view of a contact, sent as the {@code contact.updated} webhook payload. {@code
 * contact} holds {@code id}/{@code displayName} plus the workspace's custom field keys flattened
 * in, so its shape isn't fixed at compile time — built via {@code ContactSnapshotBuilder}.
 */
public record ContactSnapshot(Map<String, Object> contact) {}
