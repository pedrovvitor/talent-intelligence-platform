# ADR 0003: Deterministic Policy Before Ranking

- Status: Accepted
- Date: 2026-08-24

## Context

Semantic similarity is useful for discovery but cannot safely override salary, location, work-mode, or seniority requirements. An opaque single score would also be difficult to audit.

## Decision

Retrieve a broad semantic candidate set, load canonical records, apply deterministic eligibility, and only then compute the final weighted rank. Return typed evidence for every displayed score.

## Consequences

- A high semantic score cannot bypass hard constraints.
- Users can inspect how the result was produced.
- The retrieval window may exclude an otherwise eligible low-similarity job and therefore needs evaluation against labeled data.
- Policy and weighting changes are versioned product behavior and require regression tests.
