# ADR 0002: Local Embeddings and PGVector

- Status: Accepted
- Date: 2026-08-24

## Context

Semantic discovery is the core AI capability. A portfolio evaluator must run it without paid credentials, and canonical business records must not depend on a specialized external vector service.

## Decision

Generate 384-dimensional BGE-small-en-v1.5 embeddings in process through a LangChain4j adapter. Store derived vectors in PGVector alongside canonical PostgreSQL data and retrieve with cosine distance over an HNSW index.

## Consequences

- Local execution is deterministic and private.
- PostgreSQL simplifies the initial operational model.
- Model startup and embedding computation consume application resources.
- Embedding model/version and content hashes must be persisted before multiple models or background reindexing are supported.
