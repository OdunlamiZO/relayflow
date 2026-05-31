ALTER TABLE users
    ADD COLUMN receive_email_updates boolean NOT NULL DEFAULT true,
    ADD COLUMN two_factor_enabled    boolean NOT NULL DEFAULT false,
    ADD COLUMN two_factor_secret     text;

-- Short-lived challenges issued to users who have 2FA enabled. The client
-- sends back the challenge token + OTP to complete login.
CREATE TABLE two_factor_challenges (
    id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      text        NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL
);
