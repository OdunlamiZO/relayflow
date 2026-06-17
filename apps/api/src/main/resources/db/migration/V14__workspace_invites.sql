-- Workspace invite tokens sent to users before they become members.
-- State is tracked via accepted_at / revoked_at rather than a soft-delete column.

create table workspace_invites (
    id           uuid         primary key default gen_random_uuid(),
    workspace_id uuid         not null references workspaces(id),
    email        varchar(320) not null,
    invited_by   uuid         not null references users(id),
    token        uuid         not null unique default gen_random_uuid(),
    created_at   timestamptz  not null default now(),
    expires_at   timestamptz  not null,
    accepted_at  timestamptz,
    revoked_at   timestamptz
);

create index idx_workspace_invites_token        on workspace_invites(token);
create index idx_workspace_invites_workspace_id on workspace_invites(workspace_id);

-- Granular permissions that will be copied to workspace_members on accept.
create table workspace_invite_permissions (
    workspace_invite_id uuid        not null references workspace_invites(id),
    permission          varchar(40) not null,
    primary key (workspace_invite_id, permission)
);
