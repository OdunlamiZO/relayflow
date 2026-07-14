package com.relayflow.api.messaging.domain;

import java.util.Arrays;

/**
 * Contact fields RelayFlow already derives automatically from existing data — a workspace can't
 * redefine any of these as a custom field (enforced in {@code
 * MessagingService.updateContactFieldDefinitions}). Human-readable labels/descriptions for these
 * live in the frontend (see {@code RESERVED_CONTACT_FIELDS} in {@code messaging-api.ts}), mirroring
 * how {@link WorkspacePermission} carries no display copy either.
 */
public enum ReservedContactField {
    DISPLAY_NAME("displayName"),
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
