package io.odunlamizo.relayflow.workspace.domain;

// Docs: docs/codebase-components.md#workspacepermission

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Only mutating operations are gated; read access is open to every workspace member. */
public enum WorkspacePermission {
    INBOX,

    CONTACTS_DELETE,

    CONTACT_FIELDS_WRITE,

    WORKFLOWS_WRITE,

    WORKFLOWS_DELETE,

    CHANNELS_WRITE,

    CHANNELS_DELETE,

    AI_AGENT_WRITE,

    API_KEYS_WRITE,

    WEBHOOKS_WRITE,

    SECRETS_WRITE;

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
