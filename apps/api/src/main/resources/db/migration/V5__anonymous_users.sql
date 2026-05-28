alter table users
    add column is_anonymous  boolean      not null default false,
    add column last_active_at timestamptz;

create index idx_users_anonymous_created_at on users(is_anonymous, created_at)
    where is_anonymous = true;
