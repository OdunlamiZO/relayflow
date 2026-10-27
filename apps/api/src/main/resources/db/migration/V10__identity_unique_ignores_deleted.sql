drop index external_identities_channel_account_external_user_id_key;

create unique index external_identities_channel_account_external_user_id_key
    on external_identities(channel_account_id, external_user_id)
    where channel_account_id is not null and deleted_at is null;
