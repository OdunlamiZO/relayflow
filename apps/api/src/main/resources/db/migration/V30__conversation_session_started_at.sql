alter table conversations add column session_started_at timestamptz;

update conversations set session_started_at = created_at where session_started_at is null;

alter table conversations alter column session_started_at set not null;
alter table conversations alter column session_started_at set default now();
