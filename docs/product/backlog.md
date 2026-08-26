# Delivery Backlog

Tickets are ordered by dependency and intended GitHub milestone.

## v0.1.0 — Explainable vertical slice

- `TIP-001` Define product specification and non-goals. Done.
- `TIP-002` Establish ports-and-adapters backend boundaries. Done.
- `TIP-003` Persist catalog data and PGVector embeddings with Flyway. Done.
- `TIP-004` Implement deterministic eligibility and evidence-based ranking. Done.
- `TIP-005` Deliver typed React matching workspace. Done.
- `TIP-006` Add local container stack and quality gates. Done.

## v0.2.0 — Governed decisions

- `TIP-101` Add OIDC login and recruiter/admin RBAC.
- `TIP-102` Introduce tenant-scoped queries and database constraints.
- `TIP-103` Persist immutable match decisions with model, embedding, policy, and source versions.
- `TIP-104` Define retention, deletion, and data-subject workflows for candidate data. Done.
- `TIP-105` Add integration tests for tenant isolation and authorization failures.

## v0.3.0 — Agentic workflow and Generative UI

- `TIP-201` Define a versioned agent-event schema for SSE.
- `TIP-202` Implement read-only `searchJobs`, `inspectEligibility`, and `compareMatches` tools.
- `TIP-203` Add explicit approval before any state-changing recruiter action.
- `TIP-204` Render typed UI cards from an allow-listed event-to-component registry.
- `TIP-205` Add tool authorization, idempotency, timeout, and audit tests.

## v0.4.0 — Operability and efficiency

- `TIP-301` Add OpenTelemetry traces, structured JSON logs, and PII redaction tests.
- `TIP-302` Define match and ingestion SLOs with error-budget alerts.
- `TIP-303` Add tenant-safe Redis semantic cache with TTL and stampede protection.
- `TIP-304` Benchmark retrieval quality, concurrency, cache hit rate, and model cost.
- `TIP-305` Add resilience tests for database, cache, and model failure modes.

## v1.0.0 — Production release

- `TIP-401` Deliver Kubernetes manifests through Helm and GitOps.
- `TIP-402` Configure gateway, WAF, TLS, rate limits, and request-size policies.
- `TIP-403` Add SAST, SCA, secret scanning, SBOM, image signing, and admission policy.
- `TIP-404` Validate backup restoration and record measured RPO/RTO.
- `TIP-405` Run threat modeling, load testing, accessibility review, and disaster-recovery exercise.
