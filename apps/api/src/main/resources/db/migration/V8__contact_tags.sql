alter table workspaces add column contact_tag_definitions jsonb not null default '[]'::jsonb;

alter table contacts add column tags jsonb not null default '{}'::jsonb;
