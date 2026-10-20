# Backend Rules (Java 25 · Spring Boot 4.1.1 · Maven)

Applies to everything under `backend/`. The root [AGENTS.md](../AGENTS.md) rules apply too. Agent role: [.claude/agents/backend-engineer.md](../.claude/agents/backend-engineer.md).

## Modules (dependencies point inward only)

| Module | Contains | May depend on |
|---|---|---|
| `domain` | Records/VOs, aggregates, business rules, sealed `DomainException` | JDK (+ JSpecify) |
| `application` | Use cases, `port/in`, `port/out`, commands; in-memory fakes in its **test-jar** | `domain` |
| `infrastructure` | PokeAPI `@HttpExchange` client, Caffeine decorator, JPA entities/adapters, BCrypt, JWT | `application` |
| `presentation` | REST controllers, request/response DTOs, `GlobalExceptionHandler`, `SecurityFilterChain` | `application` |
| `bootstrap` | `PokedexApplication`, `@Bean` wiring of use cases, transaction decorator, Flyway, seed, `*IT` E2E, ArchUnit | all |

Base package: `com.pokedex.<module>.<context>` with contexts `catalog`, `collection`, `identity`, `shared`.

## Commands

```bash
./mvnw -q test
```
```bash
./mvnw verify
```
```bash
./mvnw -pl domain,application -Pmutation verify
```
```bash
./mvnw spotless:apply
```
```bash
./mvnw -pl bootstrap spring-boot:run -Dspring-boot.run.profiles=demo
```

## Conventions

- **Value objects** are records that check their rules in the compact constructor and throw a specific `DomainException`.
- **Use cases:** one class per use case, implementing its `port/in` interface, plain Java, built by `@Bean` methods in `bootstrap`. No `@Service` / `@Transactional` in `application`.
- **Ports** are named for what they do (`PokemonCatalogPort`, `CollectedPokemonRepository`). Adapters are named for the technology (`PokeApiCatalogAdapter`, `JpaCollectedPokemonRepositoryAdapter`).
- **Mappers** are written by hand and have their own unit tests. DTOs never cross a layer boundary.
- **Errors:** sealed exceptions → exhaustive `switch` in `GlobalExceptionHandler` → `ProblemDetail` with `type: /problems/<slug>`.
- **Time:** inject `java.time.Clock`; never call `Instant.now()` directly.
- **Persistence:** Flyway migrations `V<n>__<snake_case>.sql`, never edited once merged; `ddl-auto=validate`; no `EAGER`; `@Version` on aggregates.
- **HTTP clients:** fixed base URL from config, timeouts, `@Retryable` (5xx/IO only), `@ConcurrencyLimit`. Never follow URLs returned by PokeAPI.
- **Tests:** `*Test` = unit/slice (Surefire, no Docker); `*IT` = integration/E2E (Failsafe, owned by the test engineer). Use `@MockitoBean`, `@ServiceConnection` and `spring-security-test`; every write test uses `.with(csrf())`.

## Configuration and secrets

- `application.yml` (defaults), `application-demo.yml` (seed + Swagger), `application-offline.yml` (WireMock PokeAPI).
- Secrets only from environment variables: `JWT_SECRET` (≥ 256-bit; startup fails otherwise), `DB_URL`, `DB_USER`, `DB_PASSWORD`, `FLYWAY_USER`, `FLYWAY_PASSWORD`. See `../.env.example`.
