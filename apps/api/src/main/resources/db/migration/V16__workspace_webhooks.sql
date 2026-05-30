CREATE TABLE workspace_webhooks (
    id             uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id   uuid          NOT NULL UNIQUE REFERENCES workspaces(id),
    url            varchar(2048) NOT NULL,
    secret         varchar(512)  NOT NULL,
    enabled        boolean       NOT NULL DEFAULT true,
    created_at     timestamptz   NOT NULL DEFAULT now(),
    updated_at     timestamptz   NOT NULL DEFAULT now()
);

CREATE TABLE workspace_webhook_events (
    workspace_webhook_id uuid        NOT NULL REFERENCES workspace_webhooks(id) ON DELETE CASCADE,
    event_type           varchar(50) NOT NULL,
    PRIMARY KEY (workspace_webhook_id, event_type)
);
