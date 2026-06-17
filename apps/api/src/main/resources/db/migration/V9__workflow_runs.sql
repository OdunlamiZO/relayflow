create table workflow_run (
    id                   uuid        primary key default gen_random_uuid(),
    workflow_id          uuid        not null references workflow_definitions(id),
    workspace_id         uuid        not null references workspaces(id),
    conversation_id      uuid        not null references conversations(id),
    status               varchar(20) not null default 'RUNNING',
    started_at           timestamptz not null default now(),
    finished_at          timestamptz,
    error_message        text
);

create table workflow_run_step (
    id               uuid         primary key default gen_random_uuid(),
    run_id           uuid         not null references workflow_run(id),
    node_id          varchar(255) not null,
    node_type        varchar(50)  not null,
    status           varchar(20)  not null,
    input_snapshot   jsonb,
    output_snapshot  jsonb,
    error_message    text,
    started_at       timestamptz  not null default now(),
    finished_at      timestamptz,
    duration_ms      bigint
);

create index idx_workflow_run_workflow     on workflow_run(workflow_id);
create index idx_workflow_run_workspace    on workflow_run(workspace_id);
create index idx_workflow_run_conversation on workflow_run(conversation_id);
create index idx_workflow_run_step_run     on workflow_run_step(run_id);
