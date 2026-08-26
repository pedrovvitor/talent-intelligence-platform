# ADR 0005: OIDC Resource Server and Capability RBAC

- Status: Accepted
- Date: 2026-08-25

## Context

Matching and catalog operations need authenticated actors before tenant propagation and immutable decision audit can be trusted. A custom session or token format would add security-critical protocol code and couple the application to one identity product.

## Decision

Use OIDC Authorization Code with PKCE for the React public client and configure the Spring API as an OAuth2 JWT Resource Server. Validate signature, issuer, audience, timestamps, and subject. Convert only the explicit realm roles `recruiter` and `admin` into application authorities and deny every unspecified route.

Use a pinned Keycloak container and imported synthetic realm for reproducible local development. Keycloak remains replaceable: the production contract is standards-based claims and JWK validation, not Keycloak APIs.

## Consequences

- Authentication protocol and key rotation are delegated to maintained security components.
- The API stays stateless and can scale horizontally without server sessions.
- Authorization failures are deterministic and execute before business logic.
- Browser configuration needs exact origins and redirect URIs per environment.
- Tenant identity must be introduced as a validated claim before data isolation is complete.
- Local development mode and direct grants cannot be reused in production.
