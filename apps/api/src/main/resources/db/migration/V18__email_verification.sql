ALTER TABLE users
    ADD COLUMN email_verified boolean NOT NULL DEFAULT false;

-- Existing users signed up before verification was required — mark them verified.
UPDATE users SET email_verified = true;

CREATE TABLE email_verification_tokens (
    id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      text        NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    used_at    timestamptz
);
