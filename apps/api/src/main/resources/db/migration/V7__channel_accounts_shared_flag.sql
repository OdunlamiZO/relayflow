-- Distinguish shared-bot channels (platform-managed webhook) from custom-bot channels
-- (workspace-owner registers their own bot token and webhook).
-- Existing rows are all custom-bot channels, so the default is false.
alter table channel_accounts add column shared boolean not null default false;
