alter table workspaces add column provider_settings jsonb not null default '{}'::jsonb;
