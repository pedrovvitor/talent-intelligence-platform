# ADR 0001: Modular Monolith with Ports and Adapters

- Status: Accepted
- Date: 2026-08-24

## Context

The prototype mixed transport, matching, and storage concerns. The portfolio needs visible architecture boundaries without the operational cost of premature distributed services.

## Decision

Use a modular monolith with domain, application, and adapter layers. Application ports isolate the job catalog, semantic index, and embedding model. Business policy stays framework-independent.

## Consequences

- Domain and application rules are fast to unit test.
- Infrastructure providers can be replaced without rewriting policy.
- One deployable keeps local evaluation simple.
- Boundary discipline must be enforced in review until automated architecture tests are added.
