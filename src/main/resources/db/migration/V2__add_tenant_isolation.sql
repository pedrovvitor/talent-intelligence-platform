create table tenants (
    id uuid primary key,
    slug varchar(80) not null unique,
    display_name varchar(160) not null,
    created_at timestamptz not null default now()
);

insert into tenants (id, slug, display_name)
values ('00000000-0000-0000-0000-000000000001', 'local-demo', 'Local Demo Tenant');

alter table jobs add column tenant_id uuid;

update jobs
set tenant_id = '00000000-0000-0000-0000-000000000001'
where tenant_id is null;

alter table jobs alter column tenant_id set not null;
alter table jobs add constraint jobs_tenant_fk foreign key (tenant_id) references tenants(id);
alter table jobs add constraint jobs_tenant_id_unique unique (tenant_id, id);

alter table job_embeddings add column tenant_id uuid;

update job_embeddings as embedding
set tenant_id = job.tenant_id
from jobs as job
where embedding.job_id = job.id;

alter table job_embeddings alter column tenant_id set not null;
alter table job_embeddings drop constraint job_embeddings_job_id_fkey;
alter table job_embeddings add constraint job_embeddings_tenant_job_fk
    foreign key (tenant_id, job_id) references jobs(tenant_id, id) on delete cascade;
alter table job_embeddings add constraint job_embeddings_tenant_job_unique unique (tenant_id, job_id);

create index jobs_tenant_created_at_idx on jobs (tenant_id, created_at desc);
create index job_embeddings_tenant_model_idx on job_embeddings (tenant_id, embedding_model);
