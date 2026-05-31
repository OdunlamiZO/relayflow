-- ── user_identities ───────────────────────────────────────────────────────────
-- How a user proves who they are. A user may have multiple identities (e.g.
-- email/password + Google) sharing the same users row.

CREATE TABLE user_identities
(
    id               uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider         text        NOT NULL,
    provider_subject text        NOT NULL,
    credential       text,
    verified         boolean     NOT NULL DEFAULT false,
    created_at       timestamptz NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_subject)
);

INSERT INTO user_identities (user_id, provider, provider_subject, credential, verified)
SELECT id, provider, provider_subject, password_hash, email_verified
FROM users;

-- ── user_preferences ──────────────────────────────────────────────────────────
-- Per-user settings. Shared primary key keeps the join trivial.

CREATE TABLE user_preferences
(
    user_id               uuid        PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    receive_email_updates boolean     NOT NULL DEFAULT true,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now()
);

INSERT INTO user_preferences (user_id, receive_email_updates)
SELECT id, receive_email_updates
FROM users;

-- ── user_mfa_methods ──────────────────────────────────────────────────────────
-- One row per second-factor method per user. Adding SMS later is a new row
-- with type = 'SMS' — no schema change needed.

CREATE TABLE user_mfa_methods
(
    id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       text        NOT NULL,
    credential text,
    enabled    boolean     NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (user_id, type)
);

INSERT INTO user_mfa_methods (user_id, type, credential, enabled)
SELECT id, 'TOTP', two_factor_secret, two_factor_enabled
FROM users
WHERE two_factor_secret IS NOT NULL;

-- ── strip migrated columns from users ────────────────────────────────────────

ALTER TABLE users
    DROP COLUMN provider,
    DROP COLUMN provider_subject,
    DROP COLUMN password_hash,
    DROP COLUMN email_verified,
    DROP COLUMN receive_email_updates,
    DROP COLUMN two_factor_enabled,
    DROP COLUMN two_factor_secret;
