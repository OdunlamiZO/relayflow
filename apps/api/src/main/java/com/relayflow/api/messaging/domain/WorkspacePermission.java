package com.relayflow.api.messaging.domain;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Granular permission that can be assigned to a non-owner workspace member.
 *
 * <p>Permissions are grouped by section. Read access (list / view) is always granted to any
 * authenticated workspace member and is not gated behind a permission — only mutating operations
 * require a specific flag.
 *
 * <p>Dependency rules (enforced by {@link #validateDependencies}):
 *
 * <ul>
 *   <li>{@code WORKFLOWS_DELETE} requires {@code WORKFLOWS_WRITE}
 *   <li>{@code CHANNELS_DELETE} requires {@code CHANNELS_WRITE}
 * </ul>
 */
public enum WorkspacePermission {

    // --- Inbox ---

    /** View conversations and send messages. */
    INBOX,

    // --- Contacts ---

    /** Delete and merge contacts. */
    CONTACTS_DELETE,

    /** Define workspace contact field schema, and edit a contact's field values. */
    CONTACT_FIELDS_WRITE,

    // --- Workflows ---

    /** View, create, edit, and toggle workflows. */
    WORKFLOWS_WRITE,

    /** Delete workflows. Requires {@code WORKFLOWS_WRITE}. */
    WORKFLOWS_DELETE,

    // --- Channels (Settings) ---

    /** View and connect new channels. */
    CHANNELS_WRITE,

    /** Disconnect channels. Requires {@code CHANNELS_WRITE}. */
    CHANNELS_DELETE,

    // --- AI Agent (Settings) ---

    /** View and configure the workspace AI agent. */
    AI_AGENT_WRITE,

    // --- Integrations ---

    /** Create and revoke API keys. */
    API_KEYS_WRITE,

    /** Configure webhooks and rotate secrets. */
    WEBHOOKS_WRITE;

    /**
     * Validates that all prerequisite permissions are present alongside their dependents.
     *
     * @throws ResponseStatusException 400 if a dependent permission is present without its
     *     prerequisite
     */
    public static void validateDependencies(Set<WorkspacePermission> permissions) {
        if (permissions.contains(WORKFLOWS_DELETE) && !permissions.contains(WORKFLOWS_WRITE)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "WORKFLOWS_DELETE requires WORKFLOWS_WRITE");
        }

        if (permissions.contains(CHANNELS_DELETE) && !permissions.contains(CHANNELS_WRITE)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "CHANNELS_DELETE requires CHANNELS_WRITE");
        }
    }
}
