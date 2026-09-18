# Talent Intelligence Platform

> **Status: active development.** This portfolio project runs locally and has no hosted deployment. The `v0.x` milestones describe development progress, not production readiness. Product flows, evaluation, and operational controls are still evolving; see the [implementation backlog](https://github.com/pedrovvitor/talent-intelligence-platform/issues).


[![CI](https://github.com/pedrovvitor/talent-intelligence-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/pedrovvitor/talent-intelligence-platform/actions/workflows/ci.yml)
[![Version](https://img.shields.io/badge/version-0.2.0-174a42)](CHANGELOG.md)
[![License](https://img.shields.io/badge/license-MIT-d75d3b)](LICENSE)

An explainable candidate-to-job decision pipeline built with Kotlin, Spring Boot, LangChain4j, React, PostgreSQL, and PGVector. It combines semantic retrieval with deterministic business policies so that AI improves discovery without controlling eligibility or inventing evidence.

This is not a free-form chat. The model creates retrieval representations; typed application services evaluate real catalog data, enforce salary, location, seniority, and work-mode rules, and return recruiter-verifiable evidence.

## Why it matters

Keyword filters miss transferable experience, while opaque AI rankings create legal, operational, and trust risks. Talent Intelligence Platform demonstrates a safer pattern:

- semantic retrieval expands discovery beyond exact keywords;
- deterministic policies remain authoritative for hard constraints;
- every score is decomposed into evidence a user can inspect;
- PostgreSQL remains the transactional source of truth;
- local embeddings keep the first release reproducible and free of external model credentials.

## Current release

The `v0.2.0` development milestone implements the following vertical slice:

- job catalog write and read APIs;
- OIDC Authorization Code with PKCE and short-lived JWT access tokens;
- allow-listed recruiter/admin capabilities with fail-closed route authorization;
- signed tenant identity propagated through application ports, SQL, PGVector, and database constraints;
- local BGE-small-en-v1.5 embeddings through LangChain4j;
- PGVector cosine search with an HNSW index;
- eligibility filtering before final ranking;
- weighted semantic relevance and explicit skill coverage;
- append-only decision audit with actor, purpose, keyed source fingerprint, model/policy versions, scores, and evidence;
- tenant-scoped reproduction of the exact decision snapshot by `decisionId`;
- data classification, minimization, retention, deletion, access-logging, and redaction policy;
- typed React workspace with loading, empty, success, and error states;
- Flyway migrations, health probes, graceful shutdown, validation, and stable API errors;
- non-root, read-only application containers in Docker Compose;
- real-RSA negative HTTP tests plus Docker-backed PostgreSQL, PGVector, tenant, migration, and audit tests.

OpenTelemetry, Redis semantic cache, bounded agent tool calling, SSE Generative UI, and Kubernetes delivery are deliberately tracked as future releases. See [production readiness](docs/operations/production-readiness.md).

## On main toward v0.3.0

The unreleased commercial foundation adds a public, recruiter-readable product surface without turning AI into a chat wrapper:

- anonymous product, trust, and pricing pages;
- free candidate self-registration through a branded OIDC experience;
- least-privilege candidate matching against a server-owned public marketplace tenant;
- organization-provisioned recruiter access that prevents self-service role or tenant escalation;
- role-aware candidate/recruiter workspaces and a responsive visual system shared with Keycloak;
- documented Candidate, Recruiter Sandbox, Team, and Enterprise packaging without a premature payment dependency.

Pricing is a market hypothesis; billing, entitlements, metering, and automated organization provisioning are not implemented. See [pricing and packaging](docs/product/pricing-and-packaging.md) and [onboarding](docs/product/onboarding.md).

## Architecture

```mermaid
flowchart LR
    User[Recruiter or candidate] --> React[React + TypeScript]
    React --> IdP[OIDC Provider]
    React --> Security[JWT + RBAC boundary]
    IdP --> Security
    Security --> Web[Spring MVC adapters]
    Web --> App[Typed application services]
    App --> Policy[Deterministic eligibility policy]
    App --> EmbedPort[Embedding port]
    App --> CatalogPort[Job catalog port]
    App --> VectorPort[Semantic index port]
    App --> AuditPort[Immutable decision audit port]
    EmbedPort --> BGE[Local BGE embedding model]
    CatalogPort --> PostgreSQL[(PostgreSQL)]
    VectorPort --> PGVector[(PGVector HNSW index)]
    AuditPort --> PostgreSQL
```

The code follows ports and adapters inside a modular monolith. Domain policy has no dependency on Spring, HTTP, persistence, or model providers.

### Match request sequence

```mermaid
sequenceDiagram
    autonumber
    actor User as Recruiter
    participant UI as React Workspace
    participant IdP as OIDC Provider
    participant Security as JWT + RBAC Filter Chain
    participant API as MatchController
    participant Service as MatchingService
    participant Embed as EmbeddingGateway
    participant Model as Local BGE Model
    participant Index as PGVectorJobIndex
    participant DB as PostgreSQL + PGVector
    participant Catalog as JdbcJobCatalog
    participant Policy as EligibilityPolicy
    participant Audit as MatchDecisionAudit

    User->>UI: Open recruiter workspace
    UI->>IdP: Authorization Code request with PKCE S256
    IdP-->>UI: Authorization code
    UI->>IdP: Exchange code and verifier
    IdP-->>UI: Short-lived access token
    User->>UI: Submit typed candidate profile
    UI->>UI: Validate required fields and normalize lists
    UI->>UI: Refresh token when expiry is near
    UI->>Security: POST /api/matches with Bearer token
    Security->>IdP: Resolve signing key from JWK set when needed
    Security->>Security: Validate signature, iss, aud, exp, nbf, sub, tenant_id
    Security->>Security: Require RECRUITER or ADMIN capability
    Security->>API: Authenticated request
    API->>API: Bean Validation at trust boundary
    API->>Service: match(RequestIdentity, candidate, limit)
    Service->>Embed: embed(profile query)
    Embed->>Model: Generate 384-dimensional vector
    Model-->>Embed: Query embedding
    Embed-->>Service: Float vector
    Service->>Index: search(tenantId, vector, retrievalWindow=100)
    Index->>DB: Tenant-filtered cosine query on HNSW index
    DB-->>Index: Ranked job IDs and semantic scores
    Index-->>Service: Semantic candidates
    loop Each semantic candidate
        Service->>Catalog: findById(tenantId, jobId)
        Catalog->>DB: Read tenant-owned canonical job
        DB-->>Catalog: Transactional job record
        Catalog-->>Service: JobPosting
        Service->>Policy: evaluate(candidate, job)
        Policy-->>Service: Eligible or rejection reasons
        alt Candidate is eligible
            Service->>Service: Compute skill coverage and weighted score
            Service->>Service: Build typed evidence entries
        else Candidate is ineligible
            Service->>Service: Exclude before final ranking
        end
    end
    Service->>Service: HMAC canonical candidate input with tenant binding
    Service->>Audit: Append actor, source fingerprint, versions, scores, evidence
    Audit->>DB: Insert immutable decision aggregate
    DB-->>Audit: Decision stored or transaction fails
    Audit-->>Service: Audited
    Service-->>API: Decision ID and ranked eligible matches
    API-->>UI: 200 versioned MatchResponse with evidence
    UI->>UI: Render allow-listed result components
    UI-->>User: Explainable ranked roles
```

More detail is available in [architecture](docs/architecture/architecture.md) and the [decision records](docs/adr/).

## Run locally

Prerequisite: Docker Desktop with Compose v2.

```bash
git clone https://github.com/pedrovvitor/talent-intelligence-platform.git
cd talent-intelligence-platform
docker compose up --build
```

Open `http://localhost:3000`. The first build downloads container and Gradle/npm dependencies; subsequent starts are faster. No API key is required.

Sign in with a synthetic local account:

| Capability | Username | Password |
|---|---|---|
| Candidate | `candidate.synthetic` | `candidate-local-only` |
| Recruiter | `recruiter.synthetic` | `recruiter-local-only` |
| Admin | `admin.synthetic` | `admin-local-only` |

Keycloak is available at `http://localhost:8180`. Its development mode, local users, and bootstrap credentials are intentionally excluded from the production design.

Health endpoints:

```text
http://localhost:8080/actuator/health/liveness
http://localhost:8080/actuator/health/readiness
```

Stop the application without deleting local data:

```bash
docker compose down
```

## API example

```bash
curl --request POST http://localhost:8080/api/matches \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{
    "headline": "Senior JVM Engineer",
    "summary": "Backend engineer building reliable Kotlin and Java services",
    "skills": ["Kotlin", "Java", "Spring Boot", "PostgreSQL"],
    "seniority": "SENIOR",
    "preferredWorkModes": ["REMOTE", "HYBRID"],
    "preferredLocations": ["Fortaleza"],
    "minimumSalary": 110000,
    "limit": 5
  }'
```

`ACCESS_TOKEN` is a short-lived token issued for the API audience. The browser obtains it through Authorization Code with PKCE; the local-only `talent-intelligence-smoke` client supports deterministic command-line verification. See [authentication and authorization](docs/security/authentication-and-authorization.md).

## Engineering evidence

| Concern | Implementation |
|---|---|
| AI boundary | Embedding behind an application port; no repository or unrestricted tool access |
| Hallucination control | Scores derive only from stored jobs and deterministic calculations |
| Explainability | Typed semantic, skill, and eligibility evidence in every result |
| Decision governance | Append-only actor/source/version audit with reproducible score and evidence snapshots |
| Architecture | Domain, application, and adapters with dependency inversion |
| Data integrity | Flyway-managed PostgreSQL schema; canonical records separated from vectors |
| Tenant isolation | Signed claim, typed context, scoped catalog/vector queries, and composite database constraints |
| Retrieval | Version-ready 384-dimensional vectors and HNSW cosine index |
| Resilience | Bounded DB pool, explicit connection timeouts, probes, graceful shutdown |
| Supply chain | Locked npm graph, pinned major toolchain versions, automated dependency updates |
| Runtime security | Input validation, security headers, non-root containers, read-only filesystems |
| Quality | Strict Kotlin warnings, strict TypeScript, ESLint, unit and integration tests |
| Security verification | Real-RSA negative HTTP matrix plus Docker-backed tenant, vector, and audit invariants |

## Development

Backend requires JDK 21:

```bash
./gradlew test
./gradlew build
```

Frontend requires Node.js 24:

```bash
npm ci --prefix frontend
npm run lint --prefix frontend
npm run typecheck --prefix frontend
npm run test --prefix frontend
npm run build --prefix frontend
```

Read [CONTRIBUTING.md](CONTRIBUTING.md) and the repository-wide [agent contract](AGENTS.md) before changing code.

## Delivery roadmap

- `v0.2.0` (current): OIDC/RBAC, tenant isolation, immutable match-decision audit, model and policy version capture.
- `v0.3.0`: public onboarding and packaging, bounded agent tools, approval gates, SSE event contract, and allow-listed Generative UI.
- `v0.4.0`: Redis semantic cache, OpenTelemetry, SLO dashboards, load tests, and token/cost telemetry.
- `v1.0.0`: Kubernetes/GitOps deployment, WAF/API gateway integration, backup restore drill, and production threat-model closure.

The executable backlog is in [docs/product/backlog.md](docs/product/backlog.md).

## Documentation

- [Product specification](docs/product/product-spec.md)
- [Onboarding and access](docs/product/onboarding.md)
- [Pricing and packaging](docs/product/pricing-and-packaging.md)
- [Architecture](docs/architecture/architecture.md)
- [Match decision audit](docs/architecture/decision-audit.md)
- [Dependency policy](docs/architecture/dependency-policy.md)
- [Coding standards](docs/engineering/coding-standards.md)
- [Security model](docs/security/security-model.md)
- [Authentication and authorization](docs/security/authentication-and-authorization.md)
- [Tenant isolation](docs/security/tenant-isolation.md)
- [Security verification matrix](docs/security/security-verification.md)
- [Data governance](docs/security/data-governance.md)
- [Production readiness](docs/operations/production-readiness.md)
- [Architecture decision records](docs/adr/)

## License

Licensed under the [MIT License](LICENSE).
