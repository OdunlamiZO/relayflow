create table ai_agent_invocation_log (
    id                        uuid        primary key default gen_random_uuid(),
    workspace_id              uuid        not null references workspaces(id),
    conversation_id           uuid        not null references conversations(id),
    status                    varchar(40) not null default 'RUNNING',
    input_snapshot            jsonb,
    output_snapshot           jsonb,
    escalation_reason         text,
    triggered_workflow_run_id uuid        references workflow_run(id),
    started_at                timestamptz not null default now(),
    finished_at               timestamptz
);

create index idx_ai_invocation_conversation on ai_agent_invocation_log(conversation_id);
create index idx_ai_invocation_workspace    on ai_agent_invocation_log(workspace_id);

-- Used to check monthly usage counts efficiently
create index idx_ai_invocation_workspace_month
    on ai_agent_invocation_log(workspace_id, started_at)
    where status in ('SENT', 'DRAFTED');

-- Prevents the AI agent from claiming a conversation twice simultaneously.
-- When the service inserts with status RUNNING or CLARIFYING and a row already
-- exists for that conversation in one of those statuses, the insert fails
-- immediately — no explicit lock needed.
create unique index uq_ai_invocation_conversation_active
    on ai_agent_invocation_log(conversation_id)
    where status in ('RUNNING', 'CLARIFYING');
