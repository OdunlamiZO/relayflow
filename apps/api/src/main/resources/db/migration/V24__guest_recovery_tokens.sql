create table guest_recovery_tokens (
    id         uuid        primary key default gen_random_uuid(),
    user_id    uuid        not null references users (id) on delete cascade,
    token      text        not null unique,
    created_at timestamptz not null default now()
);

create index guest_recovery_tokens_user_id_idx on guest_recovery_tokens (user_id);
