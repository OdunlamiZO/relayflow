CREATE TABLE workflow_definitions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID       NOT NULL REFERENCES workspaces(id),
    name        VARCHAR(200) NOT NULL,
    enabled     BOOLEAN     NOT NULL DEFAULT false,
    draft_graph JSONB       NOT NULL DEFAULT '{"nodes":[],"edges":[]}',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_workflow_definitions_workspace
    ON workflow_definitions(workspace_id)
    WHERE deleted_at IS NULL;
