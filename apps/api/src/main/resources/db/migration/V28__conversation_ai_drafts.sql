create table conversation_ai_drafts (
    id                uuid        primary key default gen_random_uuid(),
    workspace_id      uuid        not null references workspaces(id),
    conversation_id   uuid        not null references conversations(id),
    invocation_log_id uuid        references ai_agent_invocation_log(id),
    proposed_reply    text        not null,
    suggested_actions jsonb       not null default '[]',
    created_at        timestamptz not null default now(),
    constraint uq_conversation_ai_draft unique (conversation_id)
);
