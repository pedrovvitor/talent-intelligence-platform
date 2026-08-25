create extension if not exists vector;

create table jobs (
    id uuid primary key,
    title varchar(160) not null,
    company varchar(160) not null,
    description text not null,
    required_skills text not null,
    seniority varchar(32) not null,
    work_mode varchar(32) not null,
    location varchar(160),
    salary_min numeric(14, 2),
    salary_max numeric(14, 2),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint valid_salary_range check (salary_min is null or salary_max is null or salary_min <= salary_max)
);

create table job_embeddings (
    job_id uuid primary key references jobs(id) on delete cascade,
    searchable_content text not null,
    embedding vector(384) not null,
    embedding_model varchar(120) not null,
    updated_at timestamptz not null default now()
);

create index job_embeddings_hnsw_cosine_idx
    on job_embeddings using hnsw (embedding vector_cosine_ops);

create index jobs_created_at_idx on jobs (created_at desc);
