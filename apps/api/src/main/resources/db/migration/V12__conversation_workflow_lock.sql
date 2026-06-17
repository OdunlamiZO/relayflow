-- Tracks whether a workflow run currently owns this conversation.
-- When true, agents cannot send messages; the workflow drives the interaction.
-- Reset to false when the workflow completes, fails, or closes the conversation.
alter table conversations
    add column locked_by_workflow boolean not null default false;
