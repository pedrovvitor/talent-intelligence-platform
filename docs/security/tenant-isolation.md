# Tenant Isolation

## Identity contract

The signed access token is the only organization-tenant source of truth. Recruiter and administrator identities require a UUID-valued `tenant_id` claim plus a stable `sub` actor claim. The API does not accept an `X-Tenant-Id` header, query parameter, request-body field, or browser-selected tenant identifier.

Candidate-only identities use a server-configured public marketplace tenant and may omit `tenant_id`. This is not a browser override: the marketplace identifier comes from application configuration and candidates receive only public job reads and new match decisions. Any identity with recruiter or administrator authority still requires the signed organization claim.

Missing organization tenant identity or any malformed tenant identity invalidates privileged authentication. A valid actor without the required capability receives a deterministic authorization failure before business logic executes.

## Enforcement path

```mermaid
sequenceDiagram
    autonumber
    actor Recruiter
    participant API as JWT Resource Server
    participant Controller as HTTP Adapter
    participant UseCase as Application Service
    participant Port as Tenant-scoped Port
    participant DB as PostgreSQL + PGVector

    Recruiter->>API: Bearer token with signed tenant_id
    API->>API: Validate signature, issuer, audience, time, subject, tenant UUID
    API->>Controller: Authenticated JwtAuthenticationToken
    Controller->>Controller: Resolve RequestIdentity(actorId, TenantId)
    Controller->>UseCase: Invoke operation with explicit TenantId
    UseCase->>Port: Tenant-scoped catalog or vector call
    Port->>DB: Parameterized query with tenant_id predicate
    DB->>DB: Enforce composite tenant/job foreign key
    DB-->>Port: Rows owned by the authenticated tenant only
    Port-->>UseCase: Typed domain records
    UseCase-->>Controller: Tenant-safe result
    Controller-->>Recruiter: Response without tenant override surface
```

## Persistence invariants

- `tenants.id` is the ownership root for organization and public marketplace catalogs.
- `jobs` stores a non-null `tenant_id` and exposes unique `(tenant_id, id)` ownership.
- `job_embeddings` stores the same non-null tenant and references `(tenant_id, job_id)` as a composite foreign key.
- Catalog reads, writes, counts, and vector searches always bind tenant identity.
- Vector search filters by both tenant and embedding model.
- Local migration assigns pre-v0.2 records to the versioned synthetic demo tenant; it does not delete catalog data.

The current control uses explicit application scoping plus database constraints. PostgreSQL row-level security is intentionally deferred until production database roles and pooled-connection context are defined; it must not be claimed as implemented.

## Cache and telemetry contract

Redis is not present in v0.2. When introduced, semantic-cache keys must be derived from `tenantId`, embedding model/version, normalized request fingerprint, and policy version. Entries require bounded TTLs, tenant-scoped invalidation, and stampede protection. No fallback may read a global or tenant-agnostic key.

Internal telemetry may record the tenant identifier, route template, status, latency, and correlation identifier. It must not record bearer tokens, raw claims, candidate payloads, embedding content, or match evidence.

## Verification

Automated tests use synthetic tenants to prove:

- tenant A cannot find or list tenant B jobs;
- tenant A vector search cannot return tenant B embeddings;
- a mismatched embedding tenant and canonical job is rejected by PostgreSQL;
- a missing or malformed `tenant_id` fails closed during JWT conversion;
- a candidate-only token without `tenant_id` resolves to the configured marketplace while a recruiter token without it fails closed;
- allowed HTTP requests receive tenant identity from the authenticated JWT.
- a signed tenant B token cannot retrieve tenant A's audited decision identifier.
