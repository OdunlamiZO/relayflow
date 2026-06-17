alter table ai_agent_invocation_log
    drop constraint ai_agent_invocation_log_conversation_id_fkey,
    add constraint ai_agent_invocation_log_conversation_id_fkey
        foreign key (conversation_id) references conversations(id) on delete cascade;

alter table conversation_ai_drafts
    drop constraint conversation_ai_drafts_conversation_id_fkey,
    add constraint conversation_ai_drafts_conversation_id_fkey
        foreign key (conversation_id) references conversations(id) on delete cascade;
