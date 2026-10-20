# ADR 0007 — Java 25 + Spring Boot 4.1.1

**Status:** Accepted · 2026-10-19

## Context
A new project should start on the current LTS JDK and the current Spring Boot line, so the reviewers see up-to-date practice.

## Decision
- **Java 25 (LTS)**: records, sealed types, pattern matching, virtual threads. **No preview features.**
- **Spring Boot 4.1.1** (Spring Framework 7, Jakarta EE 11, Jackson 3):
  - Per-module starters (`spring-boot-starter-webmvc`, the Flyway starter, per-technology test starters).
  - `@HttpExchange` client for PokeAPI.
  - Framework 7's built-in `@Retryable` / `@ConcurrencyLimit` instead of Resilience4j.
  - `@MockitoBean` and `@ServiceConnection` in tests.

## Consequences
- ✅ Fewer dependencies (resilience is built in) and modern language features.
- ⚠️ Test tools that read bytecode (JaCoCo, PIT, ArchUnit, Mockito/ByteBuddy) must support Java 25 class files. Phase 0 checks this with a sample test before any feature code.
- ⚠️ AI tools often generate Boot 2/3-era code. See the outdated-API list in `AGENTS.md`.

## Alternatives considered
- **Java 21 + Spring Boot 3.5:** more mature tooling, but an older line for a new project.
