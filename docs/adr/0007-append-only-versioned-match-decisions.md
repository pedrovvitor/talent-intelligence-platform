# ADR 0007: Append-only Versioned Match Decisions

- Status: Accepted
- Date: 2026-08-25

## Context

A displayed ranking cannot be explained later if the job catalog, eligibility policy, or embedding model changes. Persisting the candidate request would improve replayability but would also retain restricted free text and profile attributes beyond the request purpose.

## Decision

Create one tenant-bound audit aggregate for every completed matching request, including requests with zero eligible results. Store the authenticated actor, timestamp, policy version, embedding model, nullable generative-model and prompt versions, ranked score snapshots, job display snapshots, and ordered evidence.

Represent candidate input only as a tenant-bound HMAC-SHA-256 fingerprint over a canonical, length-prefixed payload. Store the managed-key version beside the digest. The raw candidate request and query embedding remain transient.

Expose the generated decision identifier in the match response and a tenant-scoped read endpoint for the immutable snapshot. Application ports expose append and read operations only. PostgreSQL rejects updates to decision, result, and evidence rows; deletion remains reserved for the privileged retention lifecycle defined by governance policy.

## Consequences

- A recruiter can retrieve exactly the scores, evidence, titles, and model/policy versions originally displayed.
- Catalog changes cannot rewrite historical decision presentation.
- Candidate payloads cannot be reconstructed from the audit database.
- Key rotation remains interpretable because each digest records its key version.
- Audit insertion participates in the matching transaction; a failed audit write fails the request instead of returning an unaudited decision.
- A future generative model must populate model and prompt versions rather than overloading the embedding version.
