-- Subscription row for every workspace.
-- Payment-provider columns are nullable — populated when a workspace upgrades to a paid plan.
CREATE TABLE workspace_subscriptions
(
    id                           UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    workspace_id                 UUID        NOT NULL REFERENCES workspaces (id),
    plan                         VARCHAR(32) NOT NULL DEFAULT 'FREE',
    status                       VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    payment_provider             VARCHAR(50),
    payment_customer_code        VARCHAR(255),
    payment_subscription_code    VARCHAR(255),
    payment_subscription_token   VARCHAR(255),
    current_period_end           TIMESTAMPTZ,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    downgrade_locked_channels    INT         NOT NULL DEFAULT 0,
    downgrade_locked_workflows   INT         NOT NULL DEFAULT 0,
    CONSTRAINT uq_workspace_subscriptions_workspace UNIQUE (workspace_id)
);

-- Backfill: every existing workspace starts on the FREE plan.
INSERT INTO workspace_subscriptions (workspace_id, plan, status)
SELECT id, 'FREE', 'ACTIVE'
FROM workspaces
WHERE deleted_at IS NULL
ON CONFLICT (workspace_id) DO NOTHING;
