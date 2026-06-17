alter table users
    add column email_verified boolean not null default false;

-- Existing users signed up before verification was required — mark them verified.
update users set email_verified = true;

create table email_verification_tokens (
    id         uuid        primary key default gen_random_uuid(),
    user_id    uuid        not null references users (id) on delete cascade,
    token      text        not null unique,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    used_at    timestamptz
);
