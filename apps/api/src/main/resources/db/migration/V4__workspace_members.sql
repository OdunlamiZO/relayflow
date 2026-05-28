create table workspace_members (
    id uuid primary key default gen_random_uuid(),
    workspace_id uuid not null references workspaces(id),
    user_id uuid not null references users(id),
    role varchar(40) not null default 'OWNER',
    joined_at timestamptz not null default now(),
    unique (workspace_id, user_id)
);

create index idx_workspace_members_user_id on workspace_members(user_id);
