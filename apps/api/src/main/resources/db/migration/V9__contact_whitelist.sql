alter table workspaces add column contact_access varchar(20) not null default 'ALL';
alter table workspaces add column phone_region varchar(2);
alter table external_identities add column verified_phone_number varchar(20);

create table whitelisted_phone_numbers (
    id              uuid primary key default gen_random_uuid(),
    workspace_id    uuid not null references workspaces(id),
    phone_number    varchar(20) not null,
    created_at      timestamptz not null default now(),
    constraint uq_whitelisted_phone_number unique (workspace_id, phone_number)
);
