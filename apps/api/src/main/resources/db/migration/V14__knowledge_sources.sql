create table knowledge_sources (
    id              uuid primary key default gen_random_uuid(),
    workspace_id    uuid not null references workspaces(id),
    name            varchar(100) not null,
    url             text not null,
    headers         jsonb not null default '[]'::jsonb,
    cache_seconds   integer not null default 60,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    constraint uq_knowledge_source_name unique (workspace_id, name)
);

create index idx_knowledge_sources_workspace_id on knowledge_sources(workspace_id);
