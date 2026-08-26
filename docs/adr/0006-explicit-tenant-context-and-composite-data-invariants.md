# ADR 0006: Explicit Tenant Context and Composite Data Invariants

- Status: Accepted
- Date: 2026-08-25

## Context

Tenant isolation must survive mistakes at individual layers. A client-selected tenant header is forgeable, hidden request context makes application behavior difficult to test, and query filters alone cannot prevent an embedding from referencing another tenant's job.

## Decision

Accept tenant identity only from the signed `tenant_id` JWT claim and validate it as a UUID during authentication. Resolve the actor and tenant at the HTTP boundary, then pass a typed `TenantId` explicitly through application ports and persistence adapters.

Every catalog and vector query includes the tenant predicate. PostgreSQL owns the final invariant through composite tenant/job uniqueness and a composite foreign key from embeddings to canonical jobs. Missing or malformed identity fails closed before a use case runs.

Application-level scoping is the primary control for the current modular monolith. PostgreSQL row-level security is deferred until production connection roles and safe pooled-connection tenant context are designed and tested.

## Consequences

- Tenant ownership is visible in method signatures and straightforward to test.
- A vector cannot reference a job owned by another tenant, even through direct SQL.
- Existing local data migrates to an explicit synthetic demo tenant without deletion.
- Every new persistence port must include tenant identity and an isolation test.
- Future caches must include tenant and model identity in keys and must never share entries across tenants.
- RLS remains a potential defense-in-depth layer, not an assumed control.
