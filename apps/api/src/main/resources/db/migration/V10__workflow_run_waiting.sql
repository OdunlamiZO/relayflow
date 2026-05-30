-- Supports pausing a workflow run at a "Wait for Reply" node and
-- resuming it when the contact sends a reply message.

ALTER TABLE workflow_run
    ADD COLUMN waiting_at_node_id VARCHAR(255),
    ADD COLUMN context_snapshot    JSONB;
