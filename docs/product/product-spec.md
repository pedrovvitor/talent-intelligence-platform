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
