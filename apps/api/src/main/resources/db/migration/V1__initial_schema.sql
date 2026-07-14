-- ── Core messaging ────────────────────────────────────────────────────────────

create table workspaces (
    id                          uuid        primary key default gen_random_uuid(),
    name                        varchar(160) not null,
    contact_field_definitions   jsonb       not null default '[]',
    created_at                  timestamptz not null default now(),
    deleted_at                  timestamptz
);

create table channel_accounts (
    id                    uuid        primary key default gen_random_uuid(),
    workspace_id          uuid        not null references workspaces(id),
    provider              varchar(40) not null,
    name                  varchar(160) not null,
    status                varchar(40) not null,
    encrypted_credentials text,
    -- Secret Telegram echoes back in the X-Telegram-Bot-Api-Secret-Token header.
    webhook_secret        varchar(128),
    metadata              jsonb       not null default '{}',
    created_at            timestamptz not null default now(),
    deleted_at            timestamptz
);

create table contacts (
    id            uuid        primary key default gen_random_uuid(),
    workspace_id  uuid        not null references workspaces(id),
    display_name  varchar(200),
    custom_fields jsonb       not null default '{}',
    created_at    timestamptz not null default now(),
    deleted_at    timestamptz
);

create table external_identities (
    id                        uuid        primary key default gen_random_uuid(),
    workspace_id              uuid        not null references workspaces(id),
    contact_id                uuid        not null references contacts(id),
    -- Scopes to a bot so the same Telegram user can exist in multiple bots.
    channel_account_id        uuid        references channel_accounts(id),
    provider                  varchar(40) not null,
    external_user_id          varchar(200) not null,
    external_conversation_id  varchar(200),
    username                  varchar(200),
    raw_profile               jsonb       not null default '{}',
    created_at                timestamptz not null default now(),
    deleted_at                timestamptz
);

create unique index external_identities_channel_account_external_user_id_key
    on external_identities(channel_account_id, external_user_id)
    where channel_account_id is not null;

create table conversations (
    id                  uuid        primary key default gen_random_uuid(),
    workspace_id        uuid        not null references workspaces(id),
    contact_id          uuid        not null references contacts(id),
    channel_account_id  uuid        not null references channel_accounts(id),
    status              varchar(40) not null,
    -- Not FK-enforced — old assignments shouldn't block membership changes.
    assignee_id         uuid,
    last_message_at     timestamptz,
    -- Workflow runs and the AI agent use these to claim exclusive ownership
    -- so replies can't race each other.
    locked_by_workflow  boolean     not null default false,
    locked_by_ai_agent  boolean     not null default false,
    -- Reset on reopen so AI agent context doesn't leak across sessions.
    session_started_at  timestamptz not null default now(),
    created_at          timestamptz not null default now(),
    deleted_at          timestamptz
);

create index idx_conversations_workspace_last_message on conversations(workspace_id, last_message_at desc);

create table messages (
    id                   uuid        primary key default gen_random_uuid(),
    workspace_id         uuid        not null references workspaces(id),
    conversation_id      uuid        not null references conversations(id),
    direction            varchar(20) not null,
    sender_type          varchar(40) not null,
    text                 text,
    provider_message_id  varchar(200),
    raw_payload          jsonb       not null default '{}',
    created_at           timestamptz not null default now(),
    deleted_at           timestamptz
);

create index idx_messages_conversation_created_at on messages(conversation_id, created_at);

-- ── Auth ──────────────────────────────────────────────────────────────────────

create table users (
    id           uuid         primary key default gen_random_uuid(),
    email        varchar(320) not null unique,
    display_name varchar(200),
    avatar_url   text,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now(),
    deleted_at   timestamptz
);

-- How a user proves who they are. A user may have multiple identities (e.g.
-- email/password + Google) sharing the same users row.
create table user_identities (
    id               uuid        primary key default gen_random_uuid(),
    user_id          uuid        not null references users(id) on delete cascade,
    provider         text        not null,
    provider_subject text        not null,
    credential       text,
    verified         boolean     not null default false,
    created_at       timestamptz not null default now(),
    unique (provider, provider_subject)
);

-- Per-user settings. Shared primary key keeps the join trivial.
create table user_preferences (
    user_id                uuid        primary key references users(id) on delete cascade,
    receive_email_updates  boolean     not null default true,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now()
);

-- One row per second-factor method per user. Adding SMS later is a new row
-- with type = 'SMS' — no schema change needed.
create table user_mfa_methods (
    id         uuid        primary key default gen_random_uuid(),
    user_id    uuid        not null references users(id) on delete cascade,
    type       text        not null,
    credential text,
    enabled    boolean     not null default false,
    created_at timestamptz not null default now(),
    unique (user_id, type)
);

create table two_factor_challenges (
    id         uuid        primary key default gen_random_uuid(),
    user_id    uuid        not null references users(id) on delete cascade,
    token      text        not null unique,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null
);

-- ── Workspace membership, invites, API keys, webhooks ────────────────────────

create table workspace_members (
    id           uuid        primary key default gen_random_uuid(),
    workspace_id uuid        not null references workspaces(id),
    user_id      uuid        not null references users(id),
    role         varchar(40) not null default 'OWNER',
    joined_at    timestamptz not null default now(),
    deleted_at   timestamptz,
    unique (workspace_id, user_id)
);

create index idx_workspace_members_user_id on workspace_members(user_id);

create table workspace_member_permissions (
    workspace_member_id uuid        not null references workspace_members(id),
    permission           varchar(40) not null,
    primary key (workspace_member_id, permission)
);

-- Invite tokens sent to users before they become members. State is tracked via
-- accepted_at / revoked_at rather than a soft-delete column. Also, the sole gate
-- for account creation after instance bootstrap — there is no open public signup.
create table workspace_invites (
    id           uuid         primary key default gen_random_uuid(),
    workspace_id uuid         not null references workspaces(id),
    email        varchar(320) not null,
    invited_by   uuid         not null references users(id),
    token        uuid         not null unique default gen_random_uuid(),
    created_at   timestamptz  not null default now(),
    expires_at   timestamptz  not null,
    accepted_at  timestamptz,
    revoked_at   timestamptz
);

create index idx_workspace_invites_token        on workspace_invites(token);
create index idx_workspace_invites_workspace_id on workspace_invites(workspace_id);

-- Granular permissions copied to workspace_members on accept.
create table workspace_invite_permissions (
    workspace_invite_id uuid        not null references workspace_invites(id),
    permission           varchar(40) not null,
    primary key (workspace_invite_id, permission)
);

create table workspace_api_keys (
    id            uuid         primary key default gen_random_uuid(),
    workspace_id  uuid         not null references workspaces(id),
    name          varchar(100) not null,
    key_prefix    varchar(12)  not null,
    key_hash      varchar(64)  not null unique,
    created_by    uuid         not null references users(id),
    created_at    timestamptz  not null default now(),
    last_used_at  timestamptz,
    revoked_at    timestamptz,
    expires_at    timestamptz
);

create index idx_workspace_api_keys_key_hash    on workspace_api_keys(key_hash);
create index idx_workspace_api_keys_workspace_id on workspace_api_keys(workspace_id);

create table workspace_webhooks (
    id           uuid         primary key default gen_random_uuid(),
    workspace_id uuid         not null references workspaces(id) unique,
    url          varchar(2048) not null,
    secret       varchar(512) not null,
    enabled      boolean      not null default true,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now()
);

create table workspace_webhook_events (
    workspace_webhook_id uuid        not null references workspace_webhooks(id) on delete cascade,
    event_type            varchar(50) not null,
    primary key (workspace_webhook_id, event_type)
);

-- ── Workflow engine ───────────────────────────────────────────────────────────

create table workflow_definitions (
    id           uuid        primary key default gen_random_uuid(),
    workspace_id uuid        not null references workspaces(id),
    name         varchar(200) not null,
    enabled      boolean     not null default false,
    draft_graph  jsonb       not null default '{"nodes": [], "edges": []}',
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    deleted_at   timestamptz
);

create index idx_workflow_definitions_workspace on workflow_definitions(workspace_id) where deleted_at is null;

create table workflow_run (
    id                  uuid        primary key default gen_random_uuid(),
    workflow_id         uuid        not null references workflow_definitions(id),
    workspace_id        uuid        not null references workspaces(id),
    conversation_id     uuid        not null references conversations(id),
    status              varchar(20) not null default 'RUNNING',
    started_at          timestamptz not null default now(),
    finished_at         timestamptz,
    error_message       text,
    -- Node ID the run is paused at (Ask Question), null otherwise.
    waiting_at_node_id  varchar(255),
    context_snapshot    jsonb,
    expires_at          timestamptz
);

create index idx_workflow_run_conversation on workflow_run(conversation_id);
create index idx_workflow_run_workflow     on workflow_run(workflow_id);
create index idx_workflow_run_workspace    on workflow_run(workspace_id);

create table workflow_run_step (
    id               uuid        primary key default gen_random_uuid(),
    run_id           uuid        not null references workflow_run(id),
    node_id          varchar(255) not null,
    node_type        varchar(50) not null,
    status           varchar(20) not null,
    input_snapshot   jsonb,
    output_snapshot  jsonb,
    error_message    text,
    started_at       timestamptz not null default now(),
    finished_at      timestamptz,
    duration_ms      bigint
);

create index idx_workflow_run_step_run on workflow_run_step(run_id);

-- ── AI agent ──────────────────────────────────────────────────────────────────

create table ai_agent_configs (
    id                  uuid        primary key default gen_random_uuid(),
    workspace_id        uuid        not null references workspaces(id),
    name                varchar(100) not null default 'AI Agent',
    enabled             boolean     not null default false,
    autonomy_ceiling    varchar(40) not null default 'DRAFT_ONLY',
    instructions        text,
    knowledge_base      jsonb       not null default '[]',
    escalation_keywords jsonb       not null default '[]',
    workflow_mappings   jsonb       not null default '[]',
    extraction_fields   jsonb       not null default '[]',
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),
    constraint uq_ai_agent_config_workspace unique (workspace_id)
);

create table ai_agent_invocation_log (
    id                          uuid        primary key default gen_random_uuid(),
    workspace_id                uuid        not null references workspaces(id),
    conversation_id              uuid        not null references conversations(id) on delete cascade,
    status                      varchar(40) not null default 'RUNNING',
    input_snapshot               jsonb,
    output_snapshot              jsonb,
    escalation_reason            text,
    triggered_workflow_run_id    uuid        references workflow_run(id),
    started_at                  timestamptz not null default now(),
    finished_at                  timestamptz
);

create index idx_ai_invocation_conversation on ai_agent_invocation_log(conversation_id);
create index idx_ai_invocation_workspace    on ai_agent_invocation_log(workspace_id);

-- Fast lookup for "invocations this workspace made this month" cost accounting.
create index idx_ai_invocation_workspace_month
    on ai_agent_invocation_log(workspace_id, started_at)
    where status in ('SENT', 'DRAFTED');

-- AiAgentInvocationSlotClaimer's concurrency guard relies on this.
create unique index uq_ai_invocation_conversation_active
    on ai_agent_invocation_log(conversation_id)
    where status in ('RUNNING', 'CLARIFYING');

create table conversation_ai_drafts (
    id                  uuid        primary key default gen_random_uuid(),
    workspace_id        uuid        not null references workspaces(id),
    conversation_id     uuid        not null references conversations(id) on delete cascade unique,
    invocation_log_id   uuid        references ai_agent_invocation_log(id),
    proposed_reply      text        not null,
    suggested_actions   jsonb       not null default '[]',
    extracted_data      jsonb       not null default '{}',
    created_at          timestamptz not null default now()
);
