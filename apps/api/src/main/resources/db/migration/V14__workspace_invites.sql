-- Workspace invite tokens sent to users before they become members.
-- State is tracked via accepted_at / revoked_at rather than a soft-delete column.

CREATE TABLE workspace_invites (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id uuid       NOT NULL REFERENCES workspaces(id),
    email       varchar(320) NOT NULL,
    invited_by  uuid        NOT NULL REFERENCES users(id),
    token       uuid        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at  timestamptz NOT NULL DEFAULT now(),
    expires_at  timestamptz NOT NULL,
    accepted_at timestamptz,
    revoked_at  timestamptz
);

CREATE INDEX idx_workspace_invites_token       ON workspace_invites(token);
CREATE INDEX idx_workspace_invites_workspace_id ON workspace_invites(workspace_id);

-- Granular permissions that will be copied to workspace_members on accept.
CREATE TABLE workspace_invite_permissions (
    workspace_invite_id uuid       NOT NULL REFERENCES workspace_invites(id),
    permission          varchar(40) NOT NULL,
    PRIMARY KEY (workspace_invite_id, permission)
);
