# Agent Operating Contract

## Mission

Build Talent Intelligence Platform as a secure, explainable candidate-to-job matching product. Optimize for correctness, evidence, operability, and recruiter-verifiable engineering quality.

## Communication

- Explain work to the user in Brazilian Portuguese.
- Write code, identifiers, commits, documentation, API contracts, logs, and UI copy in English.
- Lead with outcomes, risks, and evidence. Keep theory proportional to the decision.

## Delivery Workflow

Follow `idea -> research -> prototype -> specification -> tickets -> implementation -> review`.

- Do not implement behavior that is absent from the product specification or an accepted ticket.
- Record material architecture decisions in `docs/adr/` before or with the implementation.
- Keep semantic versioning and `CHANGELOG.md` aligned with user-visible changes.
- Preserve backward compatibility within a major version unless a ticket explicitly authorizes a breaking change.

## Architecture Boundaries

- Domain code must not depend on Spring, HTTP, persistence, vector stores, or model providers.
- Application services orchestrate use cases through ports.
- Adapters own framework, database, model, cache, and transport details.
- AI tools call application services. They never receive repositories, database clients, SQL, or unrestricted network access.
- Deterministic policies own hard constraints and side-effect authorization. Model output is advisory until validated.
- Sensitive mutations require authentication, authorization, idempotency, auditability, and explicit approval when the workflow requires it.

## Coding Rules

- Prefer small cohesive types, descriptive names, immutable values, constructor injection, and explicit result types.
- Avoid explanatory inline comments, Javadoc, and docstrings. Refactor code until intent is clear. Comments are allowed only for non-obvious constraints, external quirks, or security rationale.
- Do not use loose types, unchecked casts, wildcard domain payloads, silent fallbacks, or empty catch blocks.
- Never log secrets, resumes, raw candidate PII, prompts containing PII, embeddings, or model tool arguments containing restricted data.
- Validate all input at trust boundaries. Return stable machine-readable error codes.
- Make timeouts explicit for network, database, model, and cache operations.
- Retry only transient and idempotent operations, using bounded exponential backoff with jitter.
- Add no dependency unless it is already approved in `docs/architecture/dependency-policy.md` or the active ticket includes the dependency and rationale.

## Data and AI Rules

- PostgreSQL is the transactional source of truth. PGVector stores versioned retrieval representations, not canonical business state.
- Every cache entry requires a TTL and tenant-safe key design.
- Every asynchronous consumer must document delivery semantics and be idempotent.
- Persist model, prompt, embedding, policy, and source-data versions with AI decisions.
- AI responses must be structured and schema-validated.
- Claims shown to users must point to stored evidence. Missing evidence produces abstention or human review, never invented facts.
- React renders only allow-listed components from typed events. Never execute model-generated HTML, JavaScript, JSX, SQL, or shell commands.

## Security and Operations

- Apply least privilege. Do not grant Kubernetes API or secret access unless application behavior requires it.
- Run containers as non-root with a read-only root filesystem where supported.
- Keep liveness, readiness, and startup semantics distinct.
- Use structured logs and propagate W3C trace context. Mask PII before logs or telemetry leave the process.
- Track production-readiness requirements and evidence in `docs/operations/production-readiness.md`; do not claim planned controls as implemented.

## Required Validation

Before completing a change, run the smallest relevant set and report anything that could not run:

```text
./gradlew test
./gradlew build
npm ci --prefix frontend
npm run lint --prefix frontend
npm run typecheck --prefix frontend
npm run test --prefix frontend
npm run build --prefix frontend
docker compose config
```

For persistence changes, run integration tests against PostgreSQL/PGVector. For UI changes, render and inspect desktop and mobile states. For infrastructure changes, validate generated manifests and security contexts.

## Stop Conditions

Ask for direction before public releases, destructive migrations, new paid services, new production dependencies, externally visible breaking API changes, or security trade-offs that weaken this contract.
