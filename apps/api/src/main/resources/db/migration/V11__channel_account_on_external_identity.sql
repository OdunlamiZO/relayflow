-- Add channel_account_id to external_identities so that uniqueness is enforced
-- per bot (channel account) rather than per provider across the whole workspace.
-- Nullable to preserve existing rows that pre-date this migration.
ALTER TABLE external_identities
    ADD COLUMN channel_account_id uuid REFERENCES channel_accounts(id);

-- Remove the old workspace/provider/external_user_id unique constraint.
-- Use dynamic SQL so the drop works regardless of PostgreSQL's auto-generated name.
DO $$
DECLARE
    v_constraint_name text;
BEGIN
    SELECT conname INTO v_constraint_name
    FROM pg_constraint
    WHERE conrelid = 'external_identities'::regclass
      AND contype = 'u';

    IF v_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE external_identities DROP CONSTRAINT %I', v_constraint_name);
    END IF;
END $$;

-- New partial unique index: within a single channel account, an external user
-- can only ever map to one contact.
CREATE UNIQUE INDEX external_identities_channel_account_external_user_id_key
    ON external_identities (channel_account_id, external_user_id)
    WHERE channel_account_id IS NOT NULL;
