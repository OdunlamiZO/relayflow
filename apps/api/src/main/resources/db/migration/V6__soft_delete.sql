-- Add a soft-delete column to every table that can be logically deleted.
-- A non-null deleted_at means the row is soft-deleted and is excluded from
-- all normal queries via entity-level @SQLRestriction("deleted_at IS NULL").

ALTER TABLE users               ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE workspaces          ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE workspace_members   ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE channel_accounts    ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE contacts            ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE external_identities ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE conversations       ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE messages            ADD COLUMN deleted_at TIMESTAMPTZ;
