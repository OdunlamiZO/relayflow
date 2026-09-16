alter table workspace_webhooks
    drop constraint workspace_webhooks_workspace_id_key;

create index idx_workspace_webhooks_workspace_id on workspace_webhooks(workspace_id);
