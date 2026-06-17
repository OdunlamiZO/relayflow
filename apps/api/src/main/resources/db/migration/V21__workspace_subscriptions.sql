-- Subscription row for every workspace.
-- Payment-provider columns are nullable — populated when a workspace upgrades to a paid plan.
create table workspace_subscriptions
(
    id                           uuid        not null default gen_random_uuid() primary key,
    workspace_id                 uuid        not null references workspaces (id),
    plan                         varchar(32) not null default 'FREE',
    status                       varchar(32) not null default 'ACTIVE',
    payment_provider             varchar(50),
    payment_customer_code        varchar(255),
    payment_subscription_code    varchar(255),
    payment_subscription_token   varchar(255),
    current_period_end           timestamptz,
    created_at                   timestamptz not null default now(),
    updated_at                   timestamptz not null default now(),
    downgrade_locked_channels    int         not null default 0,
    downgrade_locked_workflows   int         not null default 0,
    constraint uq_workspace_subscriptions_workspace unique (workspace_id)
);

-- Backfill: every existing workspace starts on the FREE plan.
insert into workspace_subscriptions (workspace_id, plan, status)
select id, 'FREE', 'ACTIVE'
from workspaces
where deleted_at is null
on conflict (workspace_id) do nothing;
