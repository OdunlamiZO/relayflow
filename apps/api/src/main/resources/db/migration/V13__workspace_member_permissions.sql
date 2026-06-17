-- Granular per-member permission flags.
-- A missing row for a (workspace_member_id, permission) pair means
-- that permission is not granted. Owners bypass this table entirely.

create table workspace_member_permissions (
    workspace_member_id uuid        not null references workspace_members(id),
    permission          varchar(40) not null,
    primary key (workspace_member_id, permission)
);
