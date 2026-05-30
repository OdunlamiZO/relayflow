-- Granular per-member permission flags.
-- A missing row for a (workspace_member_id, permission) pair means
-- that permission is not granted. Owners bypass this table entirely.

CREATE TABLE workspace_member_permissions (
    workspace_member_id uuid NOT NULL REFERENCES workspace_members(id),
    permission          varchar(40) NOT NULL,
    PRIMARY KEY (workspace_member_id, permission)
);
