create table hooks (
    id              uuid primary key default gen_random_uuid(),
    workspace_id    uuid not null references workspaces(id),
    name            varchar(100) not null,
    description     varchar(500),
    expression      text not null,
    error_message   varchar(500) not null,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    constraint uq_hook_name unique (workspace_id, name)
);

create index idx_hooks_workspace_id on hooks(workspace_id);

alter table workflow_run add column reply_attempts integer not null default 0;

alter table conversations add column escalation_type varchar(40);

update conversations
set escalation_type = case
    when escalation_reason = 'LLM call failed' then 'LLM_FAILED'
    when escalation_reason = 'LLM requested escalation' then 'AI_REQUESTED'
    when escalation_reason like 'Escalation keyword matched:%' then 'KEYWORD_MATCHED'
end
where escalated_at is not null;
