create table ai_agent_configs (
    id                  uuid        primary key default gen_random_uuid(),
    workspace_id        uuid        not null references workspaces(id),
    enabled             boolean     not null default false,
    autonomy_ceiling    varchar(40) not null default 'DRAFT_ONLY',
    instructions        text,
    knowledge_base      jsonb       not null default '[]',
    escalation_keywords jsonb       not null default '[]',
    workflow_mappings   jsonb       not null default '[]',
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),
    constraint uq_ai_agent_config_workspace unique (workspace_id)
);
