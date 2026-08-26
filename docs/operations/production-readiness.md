# Production Readiness

Status values: `Implemented`, `Partial`, `Planned`, and `Not applicable`.

| Area | Control | Current status | Evidence or next release |
|---|---|---:|---|
| Edge | L7 gateway, TLS termination, rate limits, request limits | Planned | `TIP-402`, v1.0 |
| Edge | WAF and managed DNS/CDN | Planned | Deployment-specific, v1.0 |
| Observability | Health, liveness, readiness, and JVM metrics | Implemented | Spring Boot Actuator |
| Observability | Structured JSON logs with PII masking | Planned | `TIP-301`, v0.4 |
| Observability | OpenTelemetry traces and W3C propagation | Planned | `TIP-301`, v0.4 |
| Observability | RED/USE dashboards, profiling, SLOs, error budgets | Planned | `TIP-302`, v0.4 |
| Resilience | Graceful shutdown and bounded DB connection timeout | Implemented | `application.yml` |
| Resilience | Explicit remote-model timeout, retry, jitter, circuit breaker, bulkhead | Not applicable | No remote model in v0.1; required when introduced |
| Resilience | Idempotency for state-changing workflows | Planned | Required before agent mutations, v0.3 |
| Data | Bounded Hikari pool and Flyway migrations | Implemented | Runtime configuration and `db/migration` |
| Data | Expand/contract migration procedure | Partial | Rule documented; pipeline enforcement planned |
| Cache | TTL, tenant-safe keys, stampede protection, invalidation | Not applicable | Redis starts in v0.4, `TIP-303` |
| Messaging | Outbox, delivery semantics, schema versioning, DLQ | Not applicable | Add only when asynchronous ingestion is justified |
| Security | Input validation, parameterized SQL, non-root/read-only containers | Implemented | API, persistence adapters, Dockerfiles, Compose |
| Security | JWT/OIDC and recruiter/admin RBAC | Implemented | Resource Server, PKCE browser client, Keycloak realm, and authorization tests in `TIP-101` |
| Security | Tenant isolation | Implemented | Signed tenant claim, explicit application context, scoped SQL/vector queries, composite constraints, and Docker-backed isolation tests |
| Security | Secrets manager, TLS/mTLS, encryption at rest | Planned | Production environment, v1.0 |
| Security | SAST, DAST, SCA, secret scanning, SBOM, signing | Partial | Dependency updates and CI exist; full controls in `TIP-403` |
| Deploy | Reproducible local Compose stack | Implemented | `docker-compose.yml` |
| Deploy | Rolling/canary deployment, rollback, HPA, resource policies | Planned | `TIP-401`, v1.0 |
| Deploy | IaC and GitOps reconciliation | Planned | `TIP-401`, v1.0 |
| Governance | Versioned AI decision inputs and immutable audit | Planned | `TIP-103`, v0.2 |
| Governance | Data classification, minimization, retention, deletion, and redaction policy | Partial | Policy and HTTP redaction tests implemented by `TIP-104`; runtime lifecycle automation follows persisted candidate storage |
| Recovery | Automated backups and tested restoration | Planned | `TIP-404`, v1.0 |
| Recovery | Measured RPO/RTO and disaster-recovery exercise | Planned | `TIP-404` and `TIP-405`, v1.0 |

## Service-level objectives

SLOs are intentionally not claimed before a representative load test. Version 0.4 will define availability and latency objectives for matching and catalog operations, along with burn-rate alerts that consume error budget instead of alerting on raw infrastructure noise.

## Database changes

Production migrations follow expand/contract:

1. Add backward-compatible structures.
2. Deploy code that can use both representations.
3. Backfill with an observable, restartable job.
4. Switch reads and validate invariants.
5. Remove old structures in a later release.

Destructive changes require a backup, rollback decision, measured lock impact, and explicit approval.

## Incident and recovery expectations

- Alerts include service, symptom, user impact, runbook, and correlation identifiers.
- Backups are not accepted as a control until restoration is exercised.
- Match decisions use immutable audit storage before production automation is enabled.
- Recovery documentation records dependencies, owner, escalation path, RPO, RTO, and last exercise date.
