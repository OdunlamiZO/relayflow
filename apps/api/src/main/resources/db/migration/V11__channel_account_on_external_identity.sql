-- Add channel_account_id to external_identities so that uniqueness is enforced
-- per bot (channel account) rather than per provider across the whole workspace.
-- Nullable to preserve existing rows that pre-date this migration.
alter table external_identities
    add column channel_account_id uuid references channel_accounts(id);

-- Remove the old workspace/provider/external_user_id unique constraint.
-- Use dynamic SQL so the drop works regardless of PostgreSQL's auto-generated name.
do $$
declare
    v_constraint_name text;
begin
    select conname into v_constraint_name
    from pg_constraint
    where conrelid = 'external_identities'::regclass
      and contype = 'u';

    if v_constraint_name is not null then
        execute format('alter table external_identities drop constraint %I', v_constraint_name);
    end if;
end $$;

-- New partial unique index: within a single channel account, an external user
-- can only ever map to one contact.
create unique index external_identities_channel_account_external_user_id_key
    on external_identities (channel_account_id, external_user_id)
    where channel_account_id is not null;
