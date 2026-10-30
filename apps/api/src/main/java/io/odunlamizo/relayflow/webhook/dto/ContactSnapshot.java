package io.odunlamizo.relayflow.webhook.dto;

// Docs: docs/codebase-components.md#contactsnapshot

import java.util.Map;

public record ContactSnapshot(Map<String, Object> contact) {}
