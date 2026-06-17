-- ── user_identities ───────────────────────────────────────────────────────────
-- How a user proves who they are. A user may have multiple identities (e.g.
-- email/password + Google) sharing the same users row.

create table user_identities
(
    id               uuid        primary key default gen_random_uuid(),
    user_id          uuid        not null references users (id) on delete cascade,
    provider         text        not null,
    provider_subject text        not null,
    credential       text,
    verified         boolean     not null default false,
    created_at       timestamptz not null default now(),
    unique (provider, provider_subject)
);

insert into user_identities (user_id, provider, provider_subject, credential, verified)
select id, provider, provider_subject, password_hash, email_verified
from users;

-- ── user_preferences ──────────────────────────────────────────────────────────
-- Per-user settings. Shared primary key keeps the join trivial.

create table user_preferences
(
    user_id               uuid        primary key references users (id) on delete cascade,
    receive_email_updates boolean     not null default true,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now()
);

insert into user_preferences (user_id, receive_email_updates)
select id, receive_email_updates
from users;

-- ── user_mfa_methods ──────────────────────────────────────────────────────────
-- One row per second-factor method per user. Adding SMS later is a new row
-- with type = 'SMS' — no schema change needed.

create table user_mfa_methods
(
    id         uuid        primary key default gen_random_uuid(),
    user_id    uuid        not null references users (id) on delete cascade,
    type       text        not null,
    credential text,
    enabled    boolean     not null default false,
    created_at timestamptz not null default now(),
    unique (user_id, type)
);

insert into user_mfa_methods (user_id, type, credential, enabled)
select id, 'TOTP', two_factor_secret, two_factor_enabled
from users
where two_factor_secret is not null;

-- ── strip migrated columns from users ────────────────────────────────────────

alter table users
    drop column provider,
    drop column provider_subject,
    drop column password_hash,
    drop column email_verified,
    drop column receive_email_updates,
    drop column two_factor_enabled,
    drop column two_factor_secret;
