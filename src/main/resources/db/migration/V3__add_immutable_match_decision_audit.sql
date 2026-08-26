create table match_decisions (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    actor_id varchar(255) not null,
    source_fingerprint char(64) not null,
    fingerprint_key_version varchar(80) not null,
    policy_version varchar(120) not null,
    embedding_model varchar(120) not null,
    generative_model varchar(120),
    prompt_version varchar(120),
    created_at timestamptz not null,
    constraint match_decisions_tenant_id_unique unique (tenant_id, id)
);

create table match_decision_results (
    tenant_id uuid not null,
    decision_id uuid not null,
    rank smallint not null check (rank between 1 and 20),
    job_id uuid not null,
    title_snapshot varchar(160) not null,
    company_snapshot varchar(160) not null,
    semantic_score numeric(6, 5) not null check (semantic_score between 0 and 1),
    skill_coverage numeric(6, 5) not null check (skill_coverage between 0 and 1),
    final_score numeric(6, 5) not null check (final_score between 0 and 1),
    primary key (decision_id, rank),
    constraint match_decision_results_tenant_decision_unique unique (tenant_id, decision_id, rank),
    constraint match_decision_results_decision_fk
        foreign key (tenant_id, decision_id) references match_decisions(tenant_id, id) on delete cascade
);

create table match_decision_evidence (
    tenant_id uuid not null,
    decision_id uuid not null,
    result_rank smallint not null,
    evidence_order smallint not null check (evidence_order > 0),
    evidence_type varchar(80) not null,
    label varchar(160) not null,
    evidence_value text not null,
    primary key (decision_id, result_rank, evidence_order),
    constraint match_decision_evidence_result_fk
        foreign key (tenant_id, decision_id, result_rank)
        references match_decision_results(tenant_id, decision_id, rank) on delete cascade
);

create index match_decisions_tenant_created_at_idx on match_decisions (tenant_id, created_at desc);
create index match_decisions_tenant_source_idx on match_decisions (tenant_id, source_fingerprint);

create function reject_match_audit_update() returns trigger
language plpgsql
as $$
begin
    raise exception 'Match audit records are append-only';
end;
$$;

create trigger match_decisions_reject_update
    before update on match_decisions
    for each row execute function reject_match_audit_update();

create trigger match_decision_results_reject_update
    before update on match_decision_results
    for each row execute function reject_match_audit_update();

create trigger match_decision_evidence_reject_update
    before update on match_decision_evidence
    for each row execute function reject_match_audit_update();
