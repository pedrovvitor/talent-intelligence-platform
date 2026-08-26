# Dependency Policy

Dependencies are admitted only when an approved ticket names the capability, alternatives, operational cost, license, and exit strategy. Versions are pinned directly or through an explicit BOM and committed lockfile.

## Approved runtime dependencies

| Capability | Dependency | Rationale |
|---|---|---|
| JVM application | Spring Boot 4.1 | HTTP, validation, lifecycle, health, JDBC, and configuration |
| Language | Kotlin 2.4 | Null safety, concise immutable domain modeling, Java interoperability |
| AI abstraction | LangChain4j 1.19 | Typed embedding boundary and future guarded tool support |
| Local embedding | BGE small English v1.5 quantized module | Reproducible semantic retrieval without external credentials |
| Schema migration | Spring Boot Flyway starter and Flyway PostgreSQL module | Boot-managed, ordered, reviewable PostgreSQL migrations with database-specific support |
| Transactional and vector data | PostgreSQL driver and PGVector extension | One source of truth with vector search colocated for the initial scale |
| API identity | Spring Boot OAuth2 Resource Server and Spring Security | Standards-based JWT validation and fail-closed capability authorization; replaceable through security ports and OIDC claims |
| Local identity provider | Keycloak 26.7 | Reproducible OIDC, realm roles, and token issuance for local verification; production may use any compatible managed IdP |
| Web UI | React 19 | Typed component composition and predictable rendering |
| Build and styling | Vite 8 and Tailwind CSS 4 | Fast build pipeline and constrained design tokens |
| Browser identity | Keycloak JavaScript adapter 26.2 | Authorization Code with PKCE, token refresh, and logout without custom protocol code; replaceable behind the frontend authentication boundary |

## Approved test and quality dependencies

- Spring Boot Test and JUnit Platform.
- Spring Boot Web MVC Test and Spring Security Test for HTTP authorization boundaries.
- Kotlin JUnit 5 assertions.
- Testcontainers 2.0 for PostgreSQL/PGVector integration tests.
- TypeScript ESLint with strict type-aware rules.
- Vitest, Testing Library, JSDOM, and Jest DOM.

## Update rules

- Patch updates require green CI and changelog review.
- Minor updates require release-note review and targeted regression tests.
- Major updates require an ADR or migration ticket.
- Do not use unbounded version ranges or `latest` tags in build manifests.
- Container tags must be explicit. Production delivery will pin image digests.
- Dependency exceptions expire on a recorded date and need an owner.
