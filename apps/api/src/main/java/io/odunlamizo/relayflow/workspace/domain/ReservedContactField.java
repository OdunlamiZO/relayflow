package io.odunlamizo.relayflow.workspace.domain;

// Docs: docs/codebase-components.md#reservedcontactfield

import java.util.Arrays;

/** Labels and descriptions live in RESERVED_CONTACT_FIELDS in messaging-api.ts. */
public enum ReservedContactField {
    DISPLAY_NAME("displayName"),
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    PHONE("phone"),
    EMAIL("email"),
    COUNTRY("country");

    private final String key;

    ReservedContactField(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static boolean isReserved(String candidateKey) {
        if (candidateKey == null) {
            return false;
        }

        String trimmed = candidateKey.trim();

        return Arrays.stream(values()).anyMatch(field -> field.key.equalsIgnoreCase(trimmed));
    }
}
