create table workspaces (
    id uuid primary key,
    name varchar(160) not null,
    created_at timestamptz not null default now()
);

create table channel_accounts (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id),
    provider varchar(40) not null,
    name varchar(160) not null,
    status varchar(40) not null,
    encrypted_credentials text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create table contacts (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id),
    display_name varchar(200),
    created_at timestamptz not null default now()
);

create table external_identities (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id),
    contact_id uuid not null references contacts(id),
    provider varchar(40) not null,
    external_user_id varchar(200) not null,
    external_conversation_id varchar(200),
    username varchar(200),
    raw_profile jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    unique (workspace_id, provider, external_user_id)
);

create table conversations (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id),
    contact_id uuid not null references contacts(id),
    channel_account_id uuid not null references channel_accounts(id),
    status varchar(40) not null,
    assigned_user_id uuid,
    last_message_at timestamptz,
    created_at timestamptz not null default now()
);

create table messages (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id),
    conversation_id uuid not null references conversations(id),
    direction varchar(20) not null,
    sender_type varchar(40) not null,
    text text,
    provider_message_id varchar(200),
    raw_payload jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create index idx_messages_conversation_created_at on messages(conversation_id, created_at);
create index idx_conversations_workspace_last_message on conversations(workspace_id, last_message_at desc);
