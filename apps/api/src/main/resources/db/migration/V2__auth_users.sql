create table users (
    id uuid primary key,
    email varchar(320) not null,
    display_name varchar(200),
    avatar_url text,
    provider varchar(40) not null,
    provider_subject varchar(200) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (provider, provider_subject),
    unique (email)
);
