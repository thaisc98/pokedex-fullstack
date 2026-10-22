# Testing & Quality Strategy (TDD)

> Related: [architecture.md](architecture.md) · [security.md](security.md) · [ui-ux.md](ui-ux.md) · [roadmap.md](roadmap.md)

## 1. TDD approach: two nested loops

```
 ┌───────────── OUTER LOOP (per user story) ─────────────┐
 │ 1. Write a failing API acceptance test (*IT)          │
 │   ┌──────── INNER LOOP (per rule / unit) ───────────┐ │
 │   │ RED: failing unit test → GREEN: least code →    │ │
 │   │ REFACTOR with tests green → repeat              │ │
 │   └─────────────────────────────────────────────────┘ │
 │ 2. Acceptance test passes → refactor across layers    │
 └───────────────────────────────────────────────────────┘
```

- **Domain (inside-out):** start with the value objects (`Mass`, `Tag`, `Description`), then the aggregates (`CollectedPokemon.refreshFrom` keeps custom fields; `updateCustom` checks the rules). No mocks.
- **Application (test behaviour, prefer fakes to mocks):** in-memory fakes (`InMemoryCollectedPokemonRepository`, `StubPokemonCatalog`) shipped in `application`'s **test-jar**. Use Mockito only to check that an interaction did or did not happen, e.g. "sync never calls `save` when upstream throws".
- **Visible history:** each `test:` commit comes before the matching `feat:` / `refactor:` commit (Conventional Commits).

## 2. Test pyramid and tools

| Layer | Type | Tools | What it proves | Maven phase |
|---|---|---|---|---|
| Domain | Unit | JUnit 5, AssertJ, jqwik | Value object rules, unit conversions, normalization | Surefire (`*Test`) |
| Application | Unit | JUnit 5, fakes, a little Mockito | Use-case logic, error paths, idempotency | Surefire |
| Infra: PokeAPI | Integration | WireMock + recorded fixtures | Mapping, error translation, timeouts, retry, cache hit counts | Failsafe (`*IT`) |
| Infra: persistence | Integration | `@DataJpaTest` + Testcontainers PostgreSQL (`@ServiceConnection`) + Flyway | Migrations, constraints, JSONB, optimistic locking, no N+1 queries | Failsafe |
| Presentation | Slice | `@WebMvcTest`, `@MockitoBean` inbound ports, spring-security-test | Status codes, validation → 400, ProblemDetail format, 401/403 | Surefire |
| Whole backend | E2E (API) | `@SpringBootTest(RANDOM_PORT)` + Testcontainers + WireMock + RestAssured | Full user-story flows over HTTP | Failsafe |
| Architecture | Rules | ArchUnit (+ enforcer at build time) | Layers never import what they shouldn't | Surefire |
| Frontend: slices | Unit | Vitest | `authSlice` state changes | — |
| Frontend: Server Components | E2E | Playwright | Catalog / detail pages render server-side data, `notFound()`, `error.tsx`. Vitest **does not support async Server Components** (Next.js docs), so these are E2E only | — |
| Frontend: API layer | Integration | Vitest + MSW + real `makeStore()` | Endpoints, refetch after changes, 401 → logout, ProblemDetail parsing | — |
| Frontend: components | Component | RTL + **`@testing-library/user-event`** + `renderWithProviders(ui, { preloadedState })` + MSW | Client Components: loading / empty / error / success states; logged-in vs logged-out. HeroUI uses React Aria press events, so tests use `user-event`, not `fireEvent` | — |
| Full stack | E2E (UI) | Playwright vs `docker compose --profile offline` at 375 / 768 / 1280 px | User journeys ([ui-ux §11](ui-ux.md)); **fails on any console warning or error** | CI job |
| Accessibility | Automated | `@axe-core/playwright`, Lighthouse CI | 0 serious/critical axe violations; Lighthouse a11y ≥ 90 | CI job |
| Security | Automated | Authorization-matrix test, OWASP ZAP baseline, Dependency-Check, `npm audit` | See [security.md §6](security.md) | Surefire + CI jobs |

Each frontend test builds a **fresh store** with `makeStore()`. MSW intercepts the network, so RTK Query runs its real code; its hooks are never mocked.

## 3. Coverage and quality gates

**JaCoCo `check`, per module (the build fails below these):**

| Module | Line | Branch |
|---|---|---|
| domain | 95% | 90% |
| application | 95% | 90% |
| presentation | 85% | 80% |
| infrastructure | 80% | 75% |

- The combined report (`report-aggregate` in `bootstrap`) is expected to be **≥ 85% line**.
- Excluded from coverage: `PokedexApplication.main`, `@Configuration` classes, plain DTO records.
- **Mutation testing:** PIT (`-Pmutation`) on `domain` + `application`, mutation score ≥ 75%. This checks that the tests actually catch wrong behaviour, not just that they run the lines.
- **Frontend:** Vitest v8 coverage ≥ 80%; `authSlice` and `baseQuery` at 100%.
- **Static checks:** Spotless, compiler warnings treated as errors (`-Werror`), ESLint, `tsc --strict`.

**Commands:**
```bash
./mvnw test
```
```bash
./mvnw verify
```
```bash
./mvnw -pl domain,application -Pmutation verify
```

## 4. Test fixtures

Real PokeAPI responses are recorded once into `infrastructure/src/test/resources/pokeapi/`:

| Fixture | Why it is there |
|---|---|
| `pikachu` | Happy path |
| `eevee` | Branched evolution chain (8 branches) |
| `tauros` | No evolution |
| `pichu` | Baby Pokémon |
| `deoxys-attack` | Form id > 10000; species id differs from Pokémon id |
| species with no English flavor text | Description falls back to empty |
| `celesteela` | Extreme weight (9999 hg) |

The same fixtures feed the `offline` WireMock container used for the demo.

## 5. Edge cases and failure modes

### Catalog / PokeAPI
- `page=-1`, `size=0`, `size=101`, `page=abc` → 400. A page past the end → 200 with empty `content`.
- `idOrName`: `0`, `-5`, `99999` → 404. `Pikachu` → normalized to lowercase. `mr-mime` → valid. `pika;drop` → 400.
- Name search `q`: `Pika` and `"  Mr Mime "` are normalized; names starting with `q` come before names containing it (`duck` → ducklett, psyduck, golduck); no match → `[]`; empty, missing, 51 characters or `pika;drop` → 400 (field `q`); `limit` 0 or 21 → 400 (field `limit`); upstream 5xx → 503; a second search makes no PokeAPI call (cached index); `/pokemon/search` is never treated as a Pokémon named "search".
- Null `front_default` → placeholder image. Missing official artwork → fall back to the sprite.
- Weight conversion at the extremes (0 hg, 9999 hg) always rounds the same way.
- Flavor text containing `\f` / `\n`, duplicated across game versions, or **with no English entry** → empty description.
- Genus with no English entry → `Category.UNKNOWN`.
- Evolution chains: none, linear 3-stage, branched, baby, and forms whose species id differs.
- Upstream **404** → our 404. **5xx / timeout / connection refused** → retried with backoff, then 503. **429 and other 4xx** → **not** retried (retrying a rate limit makes it worse), then 503. **Malformed JSON** → 503, and no stack trace leaks into the response. **New unknown upstream fields** → ignored.
- Cache: a second identical call makes **zero** HTTP calls (WireMock `verify(0)`). Not-found results are not cached, or only briefly. The cache size is capped.

### Sync (US03)
- A Pokémon PokeAPI doesn't have → 404, and no row is written.
- Syncing the same Pokémon twice → still one row, custom fields kept, `syncedAt` updated, 200 instead of 201.
- **Two syncs of the same id at once** → the unique-constraint violation is caught and treated as a refresh, never a 500.
- Upstream fails in the middle of a sync → transaction rolled back, no partial row.

### Update (US04)
- Missing record → 404. Empty body / malformed JSON / wrong types → 400. Wrong `Content-Type` → 415. Body over 16 KB → 413.
- An unknown property, or an attempt to change `name` / `stats` / `weight` → 400 (BR-06).
- Tags: duplicates that differ only by case → merged. 11 tags → 400. `"Fire Type!"` → 400.
- Latitude 91 / longitude −181 → 400. Invalid language tag → 400. Two names for the same language → 400.
- Blank or whitespace-only strings → 400 (checked after trimming).
- Stale `version` → 409.
- SQL-injection or `<script>` text is stored as plain text (parameterized queries) and escaped when React renders it.

### Identity
- Duplicate email, including the same email with different capitalization → 409. A password of 11 or of 65+ characters, or one on the common-password list (`password1234`) → 400.
- Wrong password and unknown email return **the same** 401 body.
- Missing, expired, tampered or `alg:none` token → 401. A `USER` calling DELETE → 403.
- Passwords and tokens never appear in logs. A test captures the log output and checks this.
- CSRF (MockMvc, per the Spring Security reference): every write test uses `.with(csrf())`; `csrf().useInvalidToken()` → 403; no token → 403. Both return a `ProblemDetail` with `type: /problems/csrf-token-invalid`.
- CSRF (IT): `GET /auth/csrf` sets an `XSRF-TOKEN` cookie that is **not** HttpOnly; login and logout **reset** the token (the old token is rejected afterwards); no response ever sets `JSESSIONID`.
- A 6th login attempt within a minute → 429 with `Retry-After`.
- Every endpoint × role combination returns the status in the [security matrix](security.md).

### Frontend
- Loading skeletons, empty states, error states with a retry button, a placeholder when an image fails to load.
- A 401 in the middle of a session → `logout()`, redirect to the login page, then back to where the user was.
- CSRF: the app calls `GET /auth/csrf` on startup and after login/logout; a 403 `csrf-token-invalid` response → fetch a new token and retry **once** (no endless loop) — tested with MSW.
- Double submit is blocked (the button is disabled while the request runs).
- The page number lives in the URL, so back/forward work.
- Layout checked at 375 / 768 / 1280 px. Full keyboard navigation and correct `alt` text.
- Zero console warnings: React keys, `act()`, controlled/uncontrolled inputs, RTK serializability checks.
