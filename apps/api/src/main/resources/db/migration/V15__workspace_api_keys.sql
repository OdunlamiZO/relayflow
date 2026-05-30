CREATE TABLE workspace_api_keys (
    id             uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id   uuid         NOT NULL REFERENCES workspaces(id),
    name           varchar(100) NOT NULL,
    key_prefix     varchar(12)  NOT NULL,
    key_hash       varchar(64)  NOT NULL UNIQUE,
    created_by     uuid         NOT NULL REFERENCES users(id),
    created_at     timestamptz  NOT NULL DEFAULT now(),
    last_used_at   timestamptz,
    revoked_at     timestamptz
);

CREATE INDEX idx_workspace_api_keys_workspace_id ON workspace_api_keys (workspace_id);
CREATE INDEX idx_workspace_api_keys_key_hash      ON workspace_api_keys (key_hash);
