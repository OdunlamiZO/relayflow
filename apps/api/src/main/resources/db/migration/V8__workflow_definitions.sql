create table workflow_definitions (
    id          uuid         primary key default gen_random_uuid(),
    workspace_id uuid        not null references workspaces(id),
    name        varchar(200) not null,
    enabled     boolean      not null default false,
    draft_graph jsonb        not null default '{"nodes":[],"edges":[]}',
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),
    deleted_at  timestamptz
);

create index idx_workflow_definitions_workspace
    on workflow_definitions(workspace_id)
    where deleted_at is null;
