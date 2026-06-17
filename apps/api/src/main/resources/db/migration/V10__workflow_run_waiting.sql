-- Supports pausing a workflow run at a "Wait for Reply" node and
-- resuming it when the contact sends a reply message.

alter table workflow_run
    add column waiting_at_node_id varchar(255),
    add column context_snapshot    jsonb;
