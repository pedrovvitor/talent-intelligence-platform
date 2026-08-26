# Changelog

All notable changes follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Candidate data classification, retention, deletion, access-logging, and redaction policy.
- Privacy regression tests that prevent rejected candidate values from leaking through validation responses.
- OIDC Authorization Code with PKCE, JWT issuer/audience validation, and recruiter/admin capability RBAC.
- A pinned local Keycloak realm with synthetic users and real-token Docker smoke coverage.

## [0.1.1] - 2026-08-25

### Fixed

- Restored Spring Boot 4 Flyway auto-configuration for PostgreSQL migrations.
- Removed an obsolete Kotlin compiler flag that failed warning-strict container builds.
- Provided the native ONNX runtime with a constrained executable temporary filesystem and a compatible C++ runtime.
- Corrected the Nginx document root and added an HTTP healthcheck for the recruiter workspace.

## [0.1.0] - 2026-08-24

### Added

- Production-shaped Kotlin and Spring Boot application foundation.
- PostgreSQL-backed job catalog and PGVector semantic retrieval.
- Local BGE embedding model for a credential-free demo.
- Deterministic eligibility policy and evidence-based match results.
- React and TypeScript recruiter workspace.
- Docker Compose development environment with health-gated dependencies.
- Engineering, architecture, security, operations, ADR, and contribution documentation.
- Backend and frontend continuous integration quality gates.

[Unreleased]: https://github.com/pedrovvitor/talent-intelligence-platform/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/pedrovvitor/talent-intelligence-platform/releases/tag/v0.1.1
[0.1.0]: https://github.com/pedrovvitor/talent-intelligence-platform/releases/tag/v0.1.0
