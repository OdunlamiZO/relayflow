create table workspace_secrets (
    id              uuid primary key default gen_random_uuid(),
    workspace_id    uuid not null references workspaces(id),
    name            varchar(100) not null,
    encrypted_value text not null,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    constraint uq_workspace_secret_name unique (workspace_id, name)
);

create index idx_workspace_secrets_workspace_id on workspace_secrets(workspace_id);
