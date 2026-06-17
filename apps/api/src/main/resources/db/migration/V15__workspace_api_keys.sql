create table workspace_api_keys (
    id           uuid         primary key default gen_random_uuid(),
    workspace_id uuid         not null references workspaces(id),
    name         varchar(100) not null,
    key_prefix   varchar(12)  not null,
    key_hash     varchar(64)  not null unique,
    created_by   uuid         not null references users(id),
    created_at   timestamptz  not null default now(),
    last_used_at timestamptz,
    revoked_at   timestamptz
);

create index idx_workspace_api_keys_workspace_id on workspace_api_keys (workspace_id);
create index idx_workspace_api_keys_key_hash      on workspace_api_keys (key_hash);
