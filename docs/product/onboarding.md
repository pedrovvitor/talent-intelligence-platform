# Onboarding and Access

## Account types

Candidate accounts are self-service. They receive the least-privilege `candidate` role and operate only against the public marketplace tenant selected by the API.

Recruiter and administrator accounts are organization-managed. A recruiter workspace owns jobs, embeddings, and audit records, so the organization provisioning flow must create the tenant and issue roles through an administrative boundary. A public registration form must never let a user select `recruiter`, `admin`, or `tenant_id`.

## Candidate registration sequence

```mermaid
sequenceDiagram
    autonumber
    actor Candidate
    participant Web as Public React Site
    participant IdP as Branded Keycloak
    participant API as Spring Security Boundary
    participant Identity as RequestIdentityResolver
    participant Match as MatchingService
    participant DB as Public Marketplace in PostgreSQL

    Candidate->>Web: Open public landing page
    Candidate->>Web: Select Create free candidate account
    Web->>IdP: Registration action with PKCE S256
    IdP->>Candidate: Render branded registration form
    Candidate->>IdP: Submit profile and password
    IdP->>IdP: Create user with default candidate role
    IdP-->>Web: Authorization code
    Web->>IdP: Exchange code and PKCE verifier
    IdP-->>Web: Short-lived token without tenant_id
    Web->>API: POST /api/matches with Bearer token
    API->>API: Validate signature, issuer, audience, time, subject, role
    API->>Identity: Resolve candidate-only identity
    Identity->>Identity: Select configured public marketplace tenant
    Identity-->>Match: RequestIdentity(actor, marketplaceTenant)
    Match->>DB: Tenant-scoped semantic retrieval and audit append
    DB-->>Match: Public catalog results
    Match-->>Web: Ranked matches with evidence
    Web-->>Candidate: Render typed match cards
```

## Recruiter onboarding sequence

```mermaid
sequenceDiagram
    autonumber
    actor Recruiter
    participant Site as Public React Site
    participant Provisioning as Organization Provisioning Boundary
    participant IdP as OIDC Provider
    participant API as Tenant-scoped API

    Recruiter->>Site: Request recruiter workspace
    Site->>Provisioning: Submit verified organization request
    Provisioning->>Provisioning: Verify organization and plan
    Provisioning->>API: Create organization tenant idempotently
    Provisioning->>IdP: Invite user with recruiter role and signed tenant attribute
    IdP-->>Recruiter: Deliver verified invitation
    Recruiter->>IdP: Complete account and MFA policy
    IdP-->>Site: Short-lived token with recruiter role and tenant_id
    Site->>API: Invoke tenant-scoped operations
```

Organization provisioning is documented but not implemented in version 0.3. The local environment provides a synthetic recruiter to make the implemented authorization path reproducible.

## Local demo identities

| Persona | Username | Password | Scope |
|---|---|---|---|
| Candidate | `candidate.synthetic` | `candidate-local-only` | Public jobs and new match decisions |
| Recruiter | `recruiter.synthetic` | `recruiter-local-only` | Demo-tenant jobs and audited matching |
| Administrator | `admin.synthetic` | `admin-local-only` | Demo-tenant catalog administration |

These credentials are local-only and contain no personal data.
