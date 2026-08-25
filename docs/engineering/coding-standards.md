# Coding Standards

## Language and naming

- Code, identifiers, commits, API fields, logs, documentation, and UI copy are written in English.
- Names express business intent. Avoid abbreviations that are not domain vocabulary.
- Conversational project support may be provided in Brazilian Portuguese.

## Clean code

- Prefer immutable values, constructor injection, small cohesive types, and explicit result contracts.
- Do not add comments, Javadoc, or docstrings that restate code behavior. Refactor until intent is visible.
- Comments are reserved for non-obvious external constraints, interoperability quirks, or security rationale.
- Do not swallow exceptions, return sentinel values for failures, or use empty catch blocks.
- Do not log secrets, raw candidate PII, prompts containing PII, embeddings, or unrestricted tool arguments.

## Kotlin and backend

- Target Java 21 and compile Kotlin warnings as errors.
- Keep domain code framework-independent.
- Validate input at every trust boundary.
- Keep transaction ownership in application use cases.
- Use parameterized SQL and bounded database pools.
- Make network, model, cache, and database timeouts explicit.
- Retry only transient, idempotent operations with bounded backoff and jitter.

## TypeScript and React

- Keep strict mode and `noUncheckedIndexedAccess` enabled.
- Never use `any`, unsafe assertions, model-generated JSX, or raw HTML injection.
- Parse transport data into typed contracts before rendering.
- Render agent events only through an allow-listed component registry.
- Cover loading, error, empty, and success states.

## Tests

- Unit-test deterministic business rules without framework startup.
- Integration-test SQL, migrations, and vector behavior against PGVector.
- Test authorization and tenant boundaries when identity is introduced.
- A bug fix includes a regression test when practical.
- Tests must be deterministic and may not call paid or mutable external AI services.

## Commits and versions

- Use Conventional Commits in English.
- Follow Semantic Versioning.
- Update `CHANGELOG.md` for externally observable changes.
- Keep each commit focused and leave all relevant quality gates green.
