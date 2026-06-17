-- Add a soft-delete column to every table that can be logically deleted.
-- A non-null deleted_at means the row is soft-deleted and is excluded from
-- all normal queries via entity-level @SQLRestriction("deleted_at IS NULL").

alter table users               add column deleted_at timestamptz;
alter table workspaces          add column deleted_at timestamptz;
alter table workspace_members   add column deleted_at timestamptz;
alter table channel_accounts    add column deleted_at timestamptz;
alter table contacts            add column deleted_at timestamptz;
alter table external_identities add column deleted_at timestamptz;
alter table conversations       add column deleted_at timestamptz;
alter table messages            add column deleted_at timestamptz;
