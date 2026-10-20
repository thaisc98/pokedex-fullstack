# Agent Instructions

Rules for any AI coding agent working in this repository (Claude Code, Copilot, Cursor…). Read [docs/architecture.md](docs/architecture.md) before changing code.

## Agent team

Three agents with separate file ownership: **backend-engineer**, **frontend-engineer**, **test-engineer**. Definitions are in [.claude/agents/](.claude/agents/); ownership, workflow and the handoff note are in [docs/agents/README.md](docs/agents/README.md). Folder rules live in [backend/AGENTS.md](backend/AGENTS.md) and [frontend/AGENTS.md](frontend/AGENTS.md).

- **Edit only the files your role owns.** Never edit a test you don't own to make it pass.
- **The API contract is `docs/api/openapi.yaml`.** Changes go through the human.
- **End every task with the handoff note.**

## Stack (pinned — do not change without an ADR)

- **Java 25**, no preview features (`--enable-preview` is not allowed)
- **Spring Boot 4.1.1** (Spring Framework 7, Jakarta EE 11, **Jackson 3**)
- **Maven** multi-module build with the wrapper (`./mvnw`). Do not add Gradle files.
- PostgreSQL + Flyway; Testcontainers 2.x for integration tests
- Frontend: **Next.js 16 (App Router)** + React 19 + TypeScript strict + **HeroUI v3** + Tailwind v4 + **Redux Toolkit / RTK Query**. Do not use TanStack Query, Vite or React Router.
- **Next.js 16 has breaking changes compared with older versions. Read the guide in `frontend/node_modules/next/dist/docs/` before writing Next.js code.**

## Layer rules

```
bootstrap → presentation, infrastructure → application → domain
```

- `domain`: JDK only (JSpecify annotations allowed). **No** `org.springframework`, `jakarta.persistence`, `tools.jackson`, `com.fasterxml.jackson` or Lombok.
- `application`: `domain` + JDK only. Use cases are plain classes, turned into beans in `bootstrap`. No `@Service`, no `@Transactional`.
- `presentation` must never import `infrastructure`, and the reverse is also forbidden.
- PokeAPI DTOs stay in `infrastructure.pokeapi`. JPA entities stay in `infrastructure.persistence.entity`. REST DTOs stay in `presentation`. Controllers return DTOs, never entities or domain objects.
- These rules are enforced by `maven-enforcer-plugin` (banned dependencies) and ArchUnit. Fix the code that breaks a rule; never loosen the rule.

## TDD rules

1. Write the failing test first. Commit it with a `test:` prefix before the `feat:` commit that makes it pass.
2. Never write a test and the code it tests in the same step without a human reviewing in between.
3. Unit tests: `*Test.java` (Surefire). Integration/E2E tests: `*IT.java` (Failsafe).
4. In application tests, prefer the in-memory fakes from `application`'s test-jar over Mockito.
5. Test names describe behaviour: `should_preserve_custom_fields_when_resyncing`.
6. Tests must not use `Thread.sleep` or live network calls. Inject `Clock` wherever time matters.

## Outdated APIs to reject (AI tools often generate these)

- `spring-boot-starter-web` → use `spring-boot-starter-webmvc`
- `@MockBean` → use `@MockitoBean`
- `WebSecurityConfigurerAdapter`, `antMatchers`, `RestTemplate`, `javax.*`
- Jackson 2 `com.fasterxml.jackson.databind.*` → Jackson 3 `tools.jackson.*`
- Resilience4j / Spring Retry → Framework 7 `@Retryable` / `@ConcurrencyLimit`
- `@DynamicPropertySource` for containers → `@ServiceConnection`
- Old-style Redux (`createStore`, `connect()`, hand-written action types) → `configureStore` / `createSlice` / RTK Query
- Next.js Pages Router (`pages/`, `getServerSideProps`, `getStaticProps`, `next/router`) → App Router (`app/`, Server Components, `next/navigation`)
- `middleware.ts` → **`proxy.ts`** (renamed in Next.js 16)
- Synchronous `params` / `searchParams` → `await props.params` / `await props.searchParams` (they are Promises)
- A module-level Redux store → `makeStore()` per request inside `StoreProvider`
- HeroUI v2 / NextUI (`@nextui-org/*`, `HeroUIProvider`, the `heroui()` plugin in `tailwind.config.js`) → HeroUI v3 (`@heroui/react` + `@heroui/styles`, no provider, CSS imports)
- `onClick` / `disabled` on HeroUI components → `onPress` / `isDisabled` (React Aria)

## Security rules (see [docs/security.md](docs/security.md))

- Deny by default. Every new endpoint needs a row in the authorization-matrix test.
- The JWT lives only in the `access_token` httpOnly cookie. Never put tokens in `localStorage`, `sessionStorage`, Redux or logs.
- CSRF stays enabled. Every non-GET request from the frontend sends `X-XSRF-TOKEN`.
- No CORS configuration: the frontend and API share one origin.
- Request DTOs reject unknown fields; response DTOs list their fields explicitly. Never bind JSON to JPA entities.
- No string-built queries. Escape `LIKE` input.
- Never follow URLs returned by PokeAPI. Extract the id and rebuild the URL against the configured base.
- Secrets only from environment variables. Never commit them, log them, or bake them into the frontend bundle.
- Frontend: no `dangerouslySetInnerHTML`, no CDN scripts, no inline scripts without the CSP nonce. URLs go through `isSafeHttpUrl`.
- `proxy.ts` only does optimistic redirects. Never put authorization decisions there.
- Server-only code imports `'server-only'`. Never put secrets in `NEXT_PUBLIC_*` variables.

## UI/UX rules (see [docs/ui-ux.md](docs/ui-ux.md))

- Use HeroUI v3 components first (see the mapping in `docs/ui-ux.md` §6.4). Build custom components only where HeroUI has none.
- Use design tokens only (HeroUI CSS variables + `bg-type-*`); no hardcoded colors or spacing values.
- Internal links go through `AppLink` (HeroUI `render` + `next/link`), never a raw `<a href>`.
- Every async view has loading, empty, error and success states.
- WCAG 2.2 AA: labelled controls, visible focus, color never the only signal, target size ≥ 24 px.
- Images have `width`/`height` and `alt` text; below-the-fold images use `loading="lazy"`.

## Conventions

- Conventional Commits (`feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `chore:`)
- Errors are RFC 9457 `ProblemDetail`
- Every new or changed dependency is pinned (or managed by the Boot parent)
- Log every substantial prompt in [docs/genai/prompt-log.md](docs/genai/prompt-log.md)
