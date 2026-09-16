-- Null means the agent uses the platform's active LLM provider (relayflow.llm platform config).
alter table ai_agent_configs
    add column llm_provider varchar(40);
