# GenAI Workflow & Critical Evaluation

> Related: [prompt-log.md](prompt-log.md) · [task-management-exercise.md](task-management-exercise.md) · [../../AGENTS.md](../../AGENTS.md)

## 1. Where GenAI is used, and where it isn't

| Activity | AI role | Human role |
|---|---|---|
| Brainstorming edge cases from the user stories | Proposes lists of cases | **Chooses which ones matter, adds missing ones, writes the failing tests** |
| Boilerplate (POMs, DTO records, Flyway DDL, mappers, Dockerfiles) | Generates it | Reviews it against §2 |
| WireMock fixtures and JSON mapping | Drafts DTOs from recorded JSON | Checks field names and units (hectograms!) |
| Making a red test pass | Suggests the smallest code that passes | Checks it passes for the right reason |
| Refactoring | Suggests extractions and renames | Accepts only if the tests stay green |
| Frontend components | Drafts components + RTL tests | Checks accessibility, state placement, the console |
| Code review | `/code-review` on each branch diff | Decides which findings to act on |
| Documentation | Drafts README sections and ADR wording | Corrects anything inaccurate |

**Ground rules**
1. `AGENTS.md` / `CLAUDE.md` state the layer rules, versions, outdated APIs and test conventions, so every prompt starts with the right constraints.
2. **The same AI step never writes both a test and the code it tests.** Otherwise the tests just mirror the implementation.
3. Small, focused prompts: one use case, one adapter or one component at a time.
4. Every substantial prompt is logged in [prompt-log.md](prompt-log.md): the prompt, the output, whether it was accepted, modified or rejected, and why.

## 2. Review checklist for AI-generated code

### Layer boundaries
- [ ] No `org.springframework`, `jakarta.persistence`, `tools.jackson`, `com.fasterxml.jackson` or Lombok imports in `domain` / `application`
- [ ] PokeAPI DTOs and JPA entities never leave `infrastructure`; controllers never return entities
- [ ] `presentation` doesn't import `infrastructure`; controllers call inbound ports only
- [ ] Business rules live in domain types, not in controllers or JPA listeners

### Over-engineering
- [ ] No interface with a single implementation unless it is a port
- [ ] No generic `BaseRepository<T>`, CQRS, event bus or factory wrapping a record constructor
- [ ] Every added dependency is needed and pinned (or managed by the Boot parent)

### Correctness and outdated APIs
- [ ] Unit conversions (hg → kg, dm → m), pagination offset (`page*size`), off-by-one errors
- [ ] None of these Boot 2/3-era APIs: `spring-boot-starter-web`, `@MockBean`, `WebSecurityConfigurerAdapter`, `antMatchers`, `RestTemplate`, `javax.*`, Jackson 2 `databind` imports, Testcontainers 1.x names, Resilience4j where `@Retryable` is enough
- [ ] No preview Java APIs (`StructuredTaskScope`) and no `--enable-preview`
- [ ] No Gradle snippets in this Maven build
- [ ] Every unfamiliar API checked against the official docs for the pinned version
- [ ] `Optional` not used as a field or parameter type; null handling for PokeAPI's nullable sprites

### Security (full list: [security.md §10](../security.md))
- [ ] Tokens never in `localStorage` / `sessionStorage` / Redux. CSRF never disabled "to make it work" (a common AI shortcut)
- [ ] No hardcoded secrets (the JWT key comes from env); no `CORS *` together with credentials; no accidental `permitAll()`
- [ ] No string-concatenated SQL; passwords and tokens never logged; the same error for every failed login

### Performance and memory
- [ ] JPA: no `EAGER` collections, no N+1 queries (use a fetch join or `@EntityGraph`), list endpoints always paginated
- [ ] HTTP: timeouts set, a limit on parallel calls, retry only on errors that can succeed on retry, with backoff
- [ ] Every cache has a size limit and a TTL; no unbounded in-memory lists; no `ThreadLocal` leaks with virtual threads

### Tests
- [ ] Assertions check results, not how mocks were wired; no mock returning a mock
- [ ] Deterministic: injected `Clock`, no `Thread.sleep`, no live network calls
- [ ] Test names describe behaviour

### Frontend (Next.js 16 + HeroUI v3)
- [ ] App Router only: no `pages/`, `getServerSideProps`, `next/router`; route protection lives in `proxy.ts`, not `middleware.ts`
- [ ] `params` / `searchParams` awaited; Server vs Client Component boundary correct (`'use client'` only where interactivity is needed)
- [ ] No secrets in `NEXT_PUBLIC_*`; server modules import `'server-only'`; no module-level Redux store
- [ ] HeroUI **v3** APIs only (no `@nextui-org/*`, no `HeroUIProvider`, no `tailwind.config.js` plugin); `onPress` / `isDisabled`, not `onClick` / `disabled`
- [ ] A HeroUI component is used where one exists (ui-ux.md §6.4) instead of a hand-built one
- [ ] No hydration warnings (they count as console errors)
- [ ] Generated code checked against `node_modules/next/dist/docs/` and the HeroUI MCP server, not the AI's memory

### Frontend (Redux Toolkit)
- [ ] Server data is never copied into slices or `useState`; it comes only from RTK Query hooks
- [ ] **Only serializable values in the store** (ISO date strings, not `Date`); no `Error` objects or Promises
- [ ] Typed hooks (`useAppSelector`) everywhere; no plain `useSelector` / `useDispatch`
- [ ] Every mutation declares `invalidatesTags`; every query declares `providesTags`
- [ ] Derived data uses memoized selectors (`createSelector`)
- [ ] No old-style Redux (`createStore`, `connect()`, hand-written action types); no `redux-persist`
- [ ] Zero console warnings (keys, effect dependencies, `act`, controlled inputs); loading, empty and error states present; accessible labels

## 3. Automated checks behind the checklist

The checklist needs human judgement. These tools catch the mechanical problems automatically:

| Tool | What it catches |
|---|---|
| `maven-enforcer-plugin` | Framework dependencies leaking into the inner layers (when the build runs) |
| ArchUnit | Imports that cross layers (when tests run) |
| Spotless + `-Werror` | Formatting problems, compiler warnings |
| JaCoCo + PIT gates | Code that isn't tested, or tests that don't catch wrong behaviour |
| ESLint + `tsc --strict` | Frontend type and lint issues |
| Playwright console guard | Any browser console warning or error |
