# ADR 0008: Candidate Self-Registration with Server-Owned Marketplace Tenancy

- Status: Accepted
- Date: 2026-08-26

## Context

The browser previously forced every visitor into Keycloak and supported only recruiter and administrator roles. Candidate self-registration needs a usable public entry point, but allowing registration input to select a role or tenant would create privilege escalation and cross-tenant access risks.

Candidate matching currently uses transient profile input and does not persist a candidate-owned profile. Public catalog discovery can therefore use a dedicated marketplace tenant while organization data remains isolated by signed tenant claims.

## Decision

- The SPA renders the anonymous product surface by default and initializes OIDC only for an explicit sign-in/registration action or an authorization callback.
- Keycloak self-registration assigns only the `candidate` realm role.
- Candidate-only tokens may omit `tenant_id`; the API maps them to a configured public marketplace tenant.
- Any identity carrying recruiter or administrator capability must provide a valid signed tenant UUID.
- Candidates may list public jobs and create match decisions, but may not retrieve decisions by identifier or mutate catalog data.
- Recruiter workspaces remain organization-provisioned and invite-based.
- The same code-owned visual identity is applied to the public application and Keycloak login theme.

## Consequences

- Candidate onboarding is self-service without trusting browser-selected authorization context.
- Public jobs are duplicated into a dedicated marketplace catalog in the local profile.
- Candidate decision history requires a future actor-scoped query before it can be exposed safely.
- Production recruiter signup requires an idempotent provisioning boundary and cannot be reduced to a Keycloak registration toggle.
- Production candidate isolation may later move from a shared marketplace tenant to actor-owned profile resources without changing organization tenancy.

## Rejected alternatives

- Letting users choose recruiter/admin during registration was rejected as direct privilege escalation.
- Accepting `tenant_id` from a form, header, or query parameter was rejected because tenancy must be server-owned and signed.
- Giving candidates tenant-wide decision reads was rejected because a guessed decision identifier could expose another candidate's audit snapshot.
- Adding a payment provider in this slice was rejected because packaging must be validated before introducing a paid external dependency.
