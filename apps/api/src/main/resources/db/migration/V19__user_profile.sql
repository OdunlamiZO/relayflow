alter table users
    add column receive_email_updates boolean not null default true,
    add column two_factor_enabled    boolean not null default false,
    add column two_factor_secret     text;

-- Short-lived challenges issued to users who have 2FA enabled. The client
-- sends back the challenge token + OTP to complete login.
create table two_factor_challenges (
    id         uuid        primary key default gen_random_uuid(),
    user_id    uuid        not null references users (id) on delete cascade,
    token      text        not null unique,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null
);
