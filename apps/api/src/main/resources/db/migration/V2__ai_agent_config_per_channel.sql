alter table ai_agent_configs
    drop constraint uq_ai_agent_config_workspace;

alter table ai_agent_configs
    add column is_default boolean not null default false;

-- Every workspace currently has exactly one config — it becomes that workspace's default.
update ai_agent_configs
    set is_default = true;

-- At most one default config per workspace.
create unique index uq_ai_agent_config_default
    on ai_agent_configs (workspace_id)
    where is_default is true;

alter table channel_accounts
    add column ai_agent_configuration_id uuid references ai_agent_configs(id) on delete set null;
