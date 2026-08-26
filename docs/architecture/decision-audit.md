# Match Decision Audit

## Audit aggregate

```mermaid
erDiagram
    TENANTS ||--o{ MATCH_DECISIONS : owns
    MATCH_DECISIONS ||--o{ MATCH_DECISION_RESULTS : contains
    MATCH_DECISION_RESULTS ||--o{ MATCH_DECISION_EVIDENCE : explains

    MATCH_DECISIONS {
        uuid id PK
        uuid tenant_id FK
        varchar actor_id
        varchar purpose
        char source_fingerprint
        varchar fingerprint_key_version
        varchar policy_version
        varchar embedding_model
        varchar generative_model nullable
        varchar prompt_version nullable
        timestamptz created_at
    }
    MATCH_DECISION_RESULTS {
        uuid decision_id PK
        smallint rank PK
        uuid job_id
        varchar title_snapshot
        varchar company_snapshot
        numeric semantic_score
        numeric skill_coverage
        numeric final_score
    }
    MATCH_DECISION_EVIDENCE {
        uuid decision_id PK
        smallint result_rank PK
        smallint evidence_order PK
        varchar evidence_type
        varchar label
        varchar evidence_value
    }
```

The audit aggregate is separate from mutable catalog records. Job identifiers retain lineage, while title and company snapshots preserve what the recruiter saw. Scores and evidence are normalized rather than serialized into an opaque document.

## Write and reproduction flow

```mermaid
sequenceDiagram
    autonumber
    participant Controller as MatchController
    participant Service as MatchingService
    participant Fingerprint as HMAC Fingerprinter
    participant Audit as MatchDecisionAudit
    participant DB as PostgreSQL

    Controller->>Service: match(RequestIdentity, candidate, limit)
    Service->>Service: Retrieve, enforce policy, rank, build evidence
    Service->>Fingerprint: fingerprint(tenantId, canonical candidate)
    Fingerprint-->>Service: digest plus key version
    Service->>Audit: append(actor, versions, digest, ranked snapshots)
    Audit->>DB: Insert decision, results, ordered evidence
    DB->>DB: Enforce tenant FKs and append-only update triggers
    Audit-->>Service: Stored in transaction
    Service-->>Controller: MatchDecision with decisionId
    Controller-->>Controller: Return versioned response

    Controller->>Service: findDecision(tenantId, decisionId)
    Service->>Audit: Tenant-scoped immutable read
    Audit->>DB: Select metadata, results, evidence by tenant
    DB-->>Audit: Original snapshot
    Audit-->>Controller: Reproduced MatchDecision
```

## Version contract

| Field | v0.2 value | Rule |
|---|---|---|
| Policy | `eligibility-policy-v1` | Change whenever eligibility behavior changes |
| Embedding model | `bge-small-en-v1.5-q` | Supplied by the embedding adapter and used for vector lookup |
| Generative model | `null` | Required when a generative model influences the decision |
| Prompt version | `null` | Required when a prompt influences the decision |
| Fingerprint key | `local-v1` locally | Identifier only; production key material belongs in a secrets manager |

The API never accepts these versions from the browser. They originate from trusted backend components.
