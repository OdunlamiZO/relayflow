alter table ai_agent_configs
    add column name varchar(100) not null default 'AI Agent';

alter table conversations
    add column locked_by_ai_agent boolean not null default false;
