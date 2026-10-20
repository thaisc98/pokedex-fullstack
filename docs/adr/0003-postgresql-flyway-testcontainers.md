# ADR 0003 — PostgreSQL + Flyway, Testcontainers in tests

**Status:** Accepted · 2026-10-19

## Context
We need a relational store with a primary entity (`collected_pokemon`) and a user table. It needs real constraints (unique `pokedex_id`, unique email), optimistic locking and JSONB for the read-only snapshot.

## Decision
- PostgreSQL 17 in Docker Compose.
- Flyway versioned migrations (`V1__users.sql`, `V2__collected_pokemon.sql`); `ddl-auto=validate`.
- Integration tests run against PostgreSQL in Testcontainers, connected with `@ServiceConnection`.

## Consequences
- ✅ Tests use the same database engine as production. JSONB, `ON CONFLICT` and constraint behaviour are real.
- ✅ The schema is versioned and reviewable.
- ➖ Integration tests need Docker (they run in Failsafe, so `./mvnw test` stays Docker-free).

## Alternatives considered
- **H2 in-memory:** faster, but it differs from PostgreSQL for JSONB and SQL dialect, so tests can pass on H2 and still fail in production.
- **Hibernate `ddl-auto=update`:** the schema isn't reviewable or reproducible.
