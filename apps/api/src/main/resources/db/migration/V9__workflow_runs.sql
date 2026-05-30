CREATE TABLE workflow_run (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id          UUID        NOT NULL REFERENCES workflow_definitions(id),
    workspace_id         UUID        NOT NULL REFERENCES workspaces(id),
    conversation_id      UUID        NOT NULL REFERENCES conversations(id),
    status               VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    started_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at          TIMESTAMPTZ,
    error_message        TEXT
);

CREATE TABLE workflow_run_step (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id           UUID        NOT NULL REFERENCES workflow_run(id),
    node_id          VARCHAR(255) NOT NULL,
    node_type        VARCHAR(50)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    input_snapshot   JSONB,
    output_snapshot  JSONB,
    error_message    TEXT,
    started_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at      TIMESTAMPTZ,
    duration_ms      BIGINT
);

CREATE INDEX idx_workflow_run_workflow    ON workflow_run(workflow_id);
CREATE INDEX idx_workflow_run_workspace   ON workflow_run(workspace_id);
CREATE INDEX idx_workflow_run_conversation ON workflow_run(conversation_id);
CREATE INDEX idx_workflow_run_step_run    ON workflow_run_step(run_id);
