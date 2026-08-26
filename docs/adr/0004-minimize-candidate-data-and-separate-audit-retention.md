# ADR 0004: Minimize Candidate Data and Separate Audit Retention

- Status: Accepted
- Date: 2026-08-25

## Context

Matching needs candidate attributes during a request, while governance needs reproducible decision evidence. Persisting raw candidate profiles or query embeddings would increase privacy scope without serving the current product requirements. Immutable decision history and data-subject deletion also need compatible lifecycle rules.

## Decision

Process candidate attributes and query embeddings in memory and do not persist them in v0.2. Persist only tenant-bound keyed source fingerprints and decision evidence in the append-only audit store. Apply a defined retention period to complete audit records and allow deletion only through a privileged lifecycle process after legal-hold checks.

HTTP errors and operational access events use allow-listed metadata and stable messages. They never include request bodies, rejected values, raw identity claims, prompts, embeddings, or candidate free text.

## Consequences

- A database compromise does not disclose raw candidate profiles from matching requests.
- Audit records can prove which input version produced a decision without reconstructing the input.
- Candidate history cannot be reopened from the platform unless a future, separately approved profile-storage capability is introduced.
- Retention deletion is an explicit privileged operation and does not weaken the no-update rule for application APIs.
- Production needs a managed key for tenant-bound source fingerprints and a lifecycle worker with deletion evidence.
