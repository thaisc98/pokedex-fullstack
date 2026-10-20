# ADR 0001 — Maven multi-module build that enforces Clean Architecture

**Status:** Accepted · 2026-10-19

## Context
The exercise is graded on Clean Architecture: separation of concerns, and business logic that does not depend on the API or the data access code. In a single-module project the layers exist only as package names, and nothing stops an `import` from crossing them.

## Decision
Split the backend into Maven modules, with dependencies pointing inward only:

`bootstrap → presentation, infrastructure → application → domain`

- `domain` and `application` have no framework dependencies. `maven-enforcer-plugin` (`bannedDependencies`) fails the build if Spring, JPA, Jackson or Lombok appears in either one.
- ArchUnit tests in `bootstrap` add package-level checks, e.g. "presentation never imports infrastructure" and "only infrastructure has `@Entity` classes".
- Use cases become beans in `bootstrap` (`@Bean` factory methods), not through `@Service`.

## Consequences
- ✅ A layer violation fails when the build runs, before any test.
- ✅ The domain and use cases can be tested with plain JUnit, fast and without Spring.
- ➖ More POM files, and a bit more wiring in `bootstrap`.
- ➖ Transactions need a decorator (`TransactionTemplate`) instead of `@Transactional` on use cases.

## Alternatives considered
- **Single module + ArchUnit only:** simpler, but violations only show up when tests run.
- **Gradle multi-project:** works just as well. Maven was chosen because it is the most widely known in Java teams and the reviewers will recognise it immediately.
