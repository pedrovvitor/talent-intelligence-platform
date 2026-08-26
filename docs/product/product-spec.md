# Product Specification

## Product statement

Talent Intelligence Platform helps recruiting teams discover relevant roles for a candidate while keeping eligibility decisions explainable and governed. It augments exact filtering with semantic retrieval, then applies deterministic rules to canonical transactional data.

## Users

- Recruiters evaluating candidate-to-role fit.
- Talent operations teams auditing decision quality.
- Candidates exploring roles without exposing resumes to third-party models.

## Version 0.1 goals

- Create and list structured job postings.
- Retrieve semantically relevant jobs from a local vector index.
- Reject candidates that violate seniority, work-mode, location, or salary constraints.
- Rank eligible jobs using a documented formula.
- Display the evidence behind every match.
- Run locally without paid services or API credentials.

## Functional requirements

### Job catalog

- A job contains title, company, description, required skills, seniority, work mode, optional location, and optional salary range.
- Creating a job writes the transactional record and its embedding in one application use case.
- Invalid or inconsistent salary ranges are rejected.

### Matching

- A candidate supplies a headline, summary, skills, seniority, work-mode preferences, location preferences, optional minimum salary, and result limit.
- The system retrieves at most 100 semantic candidates.
- Hard eligibility rules run before final ranking.
- Final score is `semantic relevance * 0.70 + required skill coverage * 0.30`.
- The response contains only persisted job data and calculated evidence.

### User interface

- The UI supports idle, loading, error, no-result, and result states.
- Dynamic result content uses typed, allow-listed React components.
- The UI never evaluates model-generated markup or code.

## Acceptance criteria

- A compatible candidate receives ranked results with semantic, skill, and eligibility evidence.
- An ineligible candidate never appears because of a high semantic score.
- Invalid input returns a stable machine-readable error response.
- A new environment creates both transactional and vector structures through Flyway.
- The backend and frontend pass their automated quality gates.
- Docker Compose starts the database, API, and web application without external credentials.

## Non-goals for version 0.1

- Automated hiring, rejection, outreach, or contract decisions.
- Resume ingestion or storage of candidate PII.
- LLM-generated explanations.
- Multi-tenancy, identity, billing, or production cloud deployment.
- Agent tool calling or model-driven side effects.

## Success measures for later pilots

- Recall@10 against a human-labeled evaluation set.
- Zero eligibility-policy violations in regression suites.
- Evidence coverage for every displayed claim.
- Match endpoint p95 latency and error rate against an agreed SLO.
- Cost per evaluated profile when remote models are introduced.

## Version 0.3 commercial foundation

Version 0.3 introduces the public product surface without weakening the governed decision boundary delivered in version 0.2.

### User journeys

- An anonymous visitor can understand the product, trust model, and pricing without being redirected to the identity provider.
- A candidate can create an account through the OIDC provider, sign in, and run matching against the public marketplace catalog.
- A recruiter can sign in to an existing tenant-scoped workspace with an organization-provisioned account.
- A recruiter without a workspace is directed to request organizational access instead of self-assigning a privileged role.
- Every authenticated user can sign out and return to the public product surface.

### Identity and authorization requirements

- Candidate self-registration is enabled and assigns only the `candidate` role.
- Self-registration never accepts a tenant identifier or a recruiter/admin role from the browser.
- Candidate-only identities without a `tenant_id` claim are mapped by the API to the configured public marketplace tenant.
- Recruiter and admin identities continue to require a signed UUID `tenant_id` claim.
- Candidates may list public jobs and submit a typed match request.
- Candidates may not create jobs, inspect tenant-wide operational endpoints, or retrieve an audited decision by identifier.
- Recruiters and admins retain their existing tenant-scoped capabilities.

### Commercial requirements

- Candidates are free forever.
- Recruiter Sandbox is free with one seat, one active role, and 50 governed evaluations per month.
- Recruiter Team is positioned at USD 49 per recruiter per month when billed annually, with 15 active roles and 2,000 governed evaluations per workspace per month.
- Enterprise pricing is custom for SSO/SCIM, regional data controls, custom retention, private connectivity, and an SLA.
- Product packaging is a market hypothesis in version 0.3. Payment collection, entitlements, metering, and automatic tenant provisioning are not implemented.
- When billing is implemented, customers buy governed evaluations rather than raw model tokens.

### Brand and experience requirements

- The public application and OIDC pages share the same name, mark, color system, tone, and trust messaging.
- Primary journeys remain keyboard accessible and usable at 320 CSS pixels.
- The UI uses typed React components and does not evaluate generated markup.
- Local demo credentials remain synthetic and are displayed only in developer documentation.

### Acceptance criteria

- Opening `/` without a session renders the public landing page and pricing instead of forcing authentication.
- Selecting candidate registration opens the branded Keycloak registration form.
- A newly registered candidate receives only candidate capabilities and can complete a match request.
- A candidate token without a tenant claim is accepted only for candidate-safe endpoints and resolves to the server-configured marketplace tenant.
- A recruiter token without a tenant claim continues to fail authentication.
- Desktop and mobile browser checks cover the landing page, sign-in page, and authenticated workspace.
- Backend, frontend, and Docker verification remain green.
