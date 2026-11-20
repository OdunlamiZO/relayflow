alter table ai_agent_configs add column handoff_message text not null
    default 'Thanks, I''ve passed this to a team member. They''ll reply here soon.';
