create table workspace_webhooks (
    id           uuid          primary key default gen_random_uuid(),
    workspace_id uuid          not null unique references workspaces(id),
    url          varchar(2048) not null,
    secret       varchar(512)  not null,
    enabled      boolean       not null default true,
    created_at   timestamptz   not null default now(),
    updated_at   timestamptz   not null default now()
);

create table workspace_webhook_events (
    workspace_webhook_id uuid        not null references workspace_webhooks(id) on delete cascade,
    event_type           varchar(50) not null,
    primary key (workspace_webhook_id, event_type)
);
