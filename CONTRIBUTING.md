# Contributing

## Workflow

1. Start from an accepted ticket in `docs/product/backlog.md` or a GitHub issue.
2. Record material decisions in `docs/adr/`.
3. Create a focused branch using `feat/`, `fix/`, `docs/`, or `chore/`.
4. Implement the smallest vertical change and its tests.
5. Run all relevant validation commands.
6. Open a pull request that explains outcome, risk, evidence, and rollback.

## Required checks

```bash
./gradlew test
./gradlew build
npm ci --prefix frontend
npm run lint --prefix frontend
npm run typecheck --prefix frontend
npm run test --prefix frontend
npm run build --prefix frontend
docker compose config
```

Persistence changes require the PGVector integration test. UI changes include desktop and mobile evidence. Infrastructure changes include generated configuration and security-context validation.

## Commit convention

Use Conventional Commits in English, for example:

```text
feat(matching): add deterministic salary eligibility
fix(web): preserve empty result state after retry
docs(architecture): record vector index decision
```

## Pull request expectations

- No unrelated changes.
- No secrets or real candidate data.
- No new dependency without an approved rationale.
- No planned control described as implemented.
- Changelog and version updated when behavior is externally observable.
