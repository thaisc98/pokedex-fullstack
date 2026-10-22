# GenAI Prompt Log

Every substantial AI interaction, with the decision taken on its output. This log is source material for the GenAI part of the presentation.

**Verdicts:** ✅ Accepted · ✏️ Modified · ❌ Rejected

---

## #001 — Architecture blueprint

- **Date:** 2026-10-19
- **Tool:** Claude Code (Claude Opus 5.5)
- **Phase:** P0 — Inception

**Prompt (summary):** Act as a Principal Software Architect. Before any code, produce a project plan and architecture blueprint following Clean Architecture and TDD. Cover: (1) domain model, (2) layer boundaries and module layout, (3) TDD and test strategy for > 85% coverage, (4) implementation phases with entry/exit criteria, (5) GenAI workflow and a checklist for reviewing AI output. The exercise PDF was attached.

**Output:** A full blueprint covering the domain model, ports, module layout, schema, API contract, test pyramid, edge cases, roadmap and review checklist. It is the source of the files in `docs/`.

**Verdict:** ✏️ Modified

| What the AI proposed | What I changed it to | Why |
|---|---|---|
| TanStack Query for server state | **Redux Toolkit + RTK Query** | One store for all state; RTK Query still gives caching and refetching after changes |
| Gradle multi-project build | **Maven multi-module build** | Familiar to the reviewers; enforcer plugin for build-time layer rules |
| Java 21 + "latest Boot GA" | **Java 25 + Spring Boot 4.1.1** | Current LTS and current Boot line |

**What I checked critically:**
- The AI pointed out that one list page needs ~41 PokeAPI calls. I confirmed this in the PokeAPI docs (the list endpoint returns only name + URL).
- The AI noted that PokeAPI weight is in hectograms. Confirmed, and added as business rule BR-02.
- Spring Boot 4 / Framework 7 claims (built-in `@Retryable`, Jackson 3 package move, `@MockitoBean`, per-module starters) are **to be confirmed against the official docs in Phase 0**. The AI was asked to hedge exact artifact names rather than invent them.
- Pushed back on over-engineering: Resilience4j was dropped in favour of the retry built into Framework 7.

---

## #002 — Review found missing UI/UX and security documentation

- **Date:** 2026-10-19
- **Tool:** Claude Code (Claude Opus 5.5)
- **Phase:** P0 — Inception

**Trigger:** While reviewing the generated docs, I noticed there was **no UI/UX design documentation** and **no documented security model** (OWASP, access levels for Spring Boot and React). Security controls were scattered across the plan instead of being defined in one place.

**Prompt (summary):** Add UI/UX documentation and document the security levels for Spring Boot and React following OWASP rules.

**Output:** `docs/security.md` (threat model, access-level matrix, backend + frontend controls, OWASP Top 10:2025 / API Top 10:2023 / ASVS 5.0 mapping, security tests), `docs/ui-ux.md` (principles, flows, wireframes, design tokens, WCAG 2.2 AA, performance budgets, acceptance criteria), ADR 0008.

**Verdict:** ✏️ Modified (accepted after review)

**What changed because of it:**
| Before | After | Why |
|---|---|---|
| JWT in `sessionStorage` | **httpOnly cookie + CSRF token**, same origin, no CORS (ADR 0008) | OWASP advises against tokens in Web Storage, where XSS can read them |
| Password 8–72 chars | **12–64 chars, no composition rules, common-password check** | Follows ASVS / NIST 800-63B |
| No rate limiting | Bucket4j on login/register → 429 | Brute force / credential stuffing (A07, API4) |
| PokeAPI links followed as returned | Extract the id, rebuild against the fixed base | SSRF (API7) / unsafe API consumption (API10) |

**Follow-up check:** the AI first flagged `csrf.spa()` as "unverified" rather than presenting it as fact. I asked what needed confirming. It was checked against the [Spring Security 7.1.1 CSRF reference](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) and the `CsrfConfigurer` source code (`@since 7.0`; JavaScript-readable cookie + `SpaCsrfTokenRequestHandler`). The full reference also showed **two gaps in our design**: (1) the token is only created when needed, so the SPA must fetch it on startup and after login/logout; (2) our custom login controller doesn't trigger Spring's automatic token reset, so we reset it ourselves. Both were added to `security.md` §4.2.1. Good example of the AI hedging instead of inventing an API, and of a human checking before relying on it.

**Lesson:** the first AI output covered architecture well but left out **cross-cutting concerns** (security, UX) unless asked for them explicitly. These are now in `AGENTS.md`, so future prompts include them by default.

---

## #003 — UI component library and frontend framework

- **Date:** 2026-10-19
- **Tool:** Claude Code (Claude Opus 5.5)
- **Phase:** P0 — Inception

**Trigger:** The UI/UX doc listed ~20 components but no library to build them with. I asked to use **HeroUI**. While that was in progress, I also switched the frontend from Vite + React Router to **Next.js**, for better route handling.

**What the AI checked before writing (instead of relying on memory):**
- HeroUI docs: **v3** is current, built on React Aria, needs React 19 + Tailwind v4, has no Provider, uses CSS-variable theming, a `toast()` API and `render`-prop router links. The component list came from `heroui.com/llms.txt`: **v3 has no Navbar**, so the header is custom.
- The Next.js docs bundled with the installed package (16.3): `middleware.ts` → **`proxy.ts`**, CSP nonces need dynamic rendering, `params` are Promises, Vitest can't test async Server Components.

**Output:** ADR 0009 (HeroUI v3), ADR 0010 (Next.js 16 App Router); rewritten `architecture.md` §3 and `ui-ux.md` §6 (HeroUI tokens and a full component mapping); updates to the security doc (nonce CSP, `proxy.ts` as optimistic only, per-request store, `next/image` allow-list), testing, roadmap, `AGENTS.md`.

**Verdict:** ✏️ Modified

| Decision | Why |
|---|---|
| Catalog = Server Components; collection/auth = RTK Query | Use what Next.js is good at for public pages while keeping Redux Toolkit where data changes. Rejected: "everything client-side", which would raise "why Next, then?" |
| `next-themes` instead of HeroUI `useTheme` | HeroUI's docs recommend it for Next.js, and only one theme controller is allowed |
| Toasts not in Redux | HeroUI's toast queue already holds that state; copying it into Redux would duplicate it |
| `proxy.ts` only checks the cookie exists | Next.js docs: Proxy is not for authorization; lesson from CVE-2025-29927 |

**Common AI mistakes this guards against:** HeroUI v2/NextUI code, Pages Router code, `middleware.ts`, synchronous `params`, a module-level Redux store. All are now in `AGENTS.md`.

---

## #004 — Splitting the work across three AI agents

- **Date:** 2026-10-20
- **Tool:** Claude Code (Claude Opus 5.5)
- **Phase:** P0 — Inception

**Prompt (summary):** I will use a backend Java agent, a frontend agent and a tester agent. Can each one have its own documentation?

**Output:** `docs/agents/README.md` (file ownership, contract-first API, per-story workflow, handoff note, definition of done, when to stop and ask); three Claude Code subagent definitions in `.claude/agents/`; folder rules in `backend/AGENTS.md` and `frontend/AGENTS.md`.

**Verdict:** ✅ Accepted

**Design choices worth presenting:**
- **The tester owns the acceptance tests (`*IT`, `e2e/`), and the developers can't edit them.** This rules out the common AI habit of changing a test until it passes, and makes "test and implementation never written in the same step" a property of the setup rather than a promise.
- **Contract first** (`docs/api/openapi.yaml`): frontend and backend can work in parallel; the frontend uses MSW mocks built from the contract.
- **`frontend/AGENTS.md` keeps Next.js's managed `nextjs-agent-rules` block** exactly as `next dev` writes it (checked against `generate-agent-files.js` in Next 16.3.8). `next dev` therefore won't rewrite the file.
- Every agent ends with a structured **handoff note**, including GenAI notes that feed this log.

---

## #005 — P0 backend: API contract, CI gates and dependency scanning with three agents

- **Date:** 2026-10-20
- **Tool:** Claude Code (Claude Opus 5.5): backend-engineer and test-engineer subagents, plus a lead session reviewing their handoffs
- **Phase:** P0 — Inception & Scaffolding

**Prompts (summary):** One focused prompt per task, each ending with the handoff note: (1) backend writes `docs/api/openapi.yaml` v1 from architecture §2.8 and security.md §3; (2) backend adds CycloneDX + OWASP Dependency-Check in a `-Psecurity` profile and proves the enforcer and ArchUnit fail on deliberate violations; (3) test engineer adds the `backend` and `dependency-scan` CI jobs and `docker-compose.yml` with PostgreSQL; plus small follow-ups (Maven wrapper checksum, Tomcat CVE override).

**Output:** PRs #5 (contract), #6 (architecture.md aligned with the contract), #7 (`-Psecurity`), #8 (CI + Compose), #9 (wrapper checksum), #10 (Tomcat override).

**Verdict:** ✏️ Modified

| What the AI did or proposed | What I changed / decided | Why |
|---|---|---|
| Contract v1 left 5 open questions and several assumptions as inline `ASSUMPTION:` comments | Decided each one: `idOrName` accepts mixed case (testing-strategy §5 says `Pikachu` is normalized), 403/413/415 on **every** write, `PokemonType` as a closed 18-value enum, `EvolutionTrigger` as a tagged union, no `multipleOf` on mass | The agent didn't guess silently. Two assumptions contradicted the docs, and `multipleOf: 0.1` breaks on floating-point values like 6.9 |
| First contract pass left the CSRF 403 off PUT | Kept the 403 on every non-GET | Spring Security's CSRF filter protects every non-GET method anyway; the contract must say so |
| Dependency-Check with the per-module `check` goal | `aggregate` goal at the root | One report for the whole backend instead of six |
| Guard proofs (enforcer, ArchUnit) on a throwaway branch | Accepted; the agent's own cleanup wiped its uncommitted POM change, and it noticed and re-applied it | Lesson: commit before any destructive experiment |
| My prompt said to take the wrapper checksum "from the official `.sha256` file" | The agent used the official `.sha512` instead (the `.sha256` file returns 404 on Maven Central) and said so in the commit | The instruction was wrong; I verified the chain independently (SHA-512 matches, SHA-256 of the same zip matches the pin) |
| CI scan job with a 45-min timeout and a cache that only saves on success | 120-min timeout, `actions/cache/restore` + `save` with `if: always()`, `-DskipTests` | The first NVD download (410,848 records) took 1 h 35 min; a timed-out run saved nothing and every retry started from zero |
| Considered switching to Trivy when NVD stalled | Kept Dependency-Check once the cache worked (cached scans take ~1 min) | The docs name Dependency-Check; the tool then caught a real problem |

**What the scan found:** 9 CVEs (CVSS 7.5–9.8) in `tomcat-embed-core 11.0.24`, the version Spring Boot 4.1.1 manages. NVD lists them as fixed in 11.0.25. Fixed with `<tomcat.version>11.0.27</tomcat.version>`, keeping Boot at 4.1.1 (pinned stack, no ADR needed); remove the override once Boot manages Tomcat ≥ 11.0.25.

**What I checked critically:**
- Plugin versions, action SHAs and the Postgres digest were looked up (Maven Central, `git ls-remote`, `docker buildx imagetools inspect`), not generated from memory.
- The test engineer added a guard so the scan job fails if the `security` profile is missing, because Maven 3.9 only warns about unknown profiles and the job would otherwise pass without scanning anything.
- An agent session's status reports went stale twice ("uncommitted skeleton on main", "#7 still open"); checking `git` and `gh` directly showed both were wrong.

**Checklist items triggered:** [workflow.md §2](workflow.md): correctness and outdated APIs (verify every unfamiliar API), security (no secrets in logs or artifacts), over-engineering (one aggregate report).

---

## #006 — P0 frontend scaffold (Next.js 16 + HeroUI v3 + Redux Toolkit)

- **Date:** 2026-10-20
- **Tool:** Claude Code (Claude Opus 5.5): frontend-engineer subagent
- **Phase:** P0 — Inception & Scaffolding

**Prompt (summary):** Scaffold `frontend/` with no features: Next.js 16 App Router + React 19 + TS strict + ESLint + Tailwind v4 in the `src/` layout of `frontend/AGENTS.md`, without letting the generator overwrite `AGENTS.md`/`CLAUDE.md`/`.env.example`; HeroUI v3 set up from the HeroUI MCP quick-start, and confirm whether `@heroui/react` ships `"use client"`; RTK skeleton (`makeStore`, `StoreProvider`, typed hooks, empty `baseApi`); Vitest + RTL + MSW with one real test; scripts incl. `gen:api`; exact-pinned versions, `engines.node` 26; placeholder home page with a HeroUI component; `output: 'standalone'`. Lint, typecheck, test and build must pass with zero warnings.

**Output:** `frontend/` skeleton (`create-next-app@16.4.2` generated into a scratch dir, then copied in), `src/store/*`, `src/shared/api/{baseApi.ts,schema.d.ts}`, `src/mocks/*`, `vitest.config.mts`, `vitest.setup.ts`, `StoreProvider.test.tsx` (4 tests), `.npmrc` (`save-exact`, `engine-strict`).

**Verdict:** ✏️ Modified

| What the AI / generator did or proposed | What was changed / decided | Why |
|---|---|---|
| `create-next-app` 16.4.2 enables `cacheComponents` + `partialPrefetching` by default | Removed for now; open question for the human | Cache Components prerenders a static shell, but our CSP needs a per-request nonce and therefore dynamic rendering (Next.js CSP guide, security.md). Needs a decision, not a generator default |
| `StoreProvider` with `useRef` (RTK Next.js guide, and the brief) | Lazy `useState(makeStore)` | `eslint-plugin-react-hooks` 7 (`react-hooks/refs`) rejects reading `ref.current` during render, in both the guide's form and a local-variable variant. Same once-per-mount guarantee; no lint rule disabled |
| MSW setup from memory would use `server.listen({ onUnhandledRequest: 'error' })` | `onUnhandledFrame: 'error'` | MSW 3 renamed the option; checked in `msw/lib/node/index.d.ts` and `shared-options.d.ts` |
| Latest majors: TypeScript 7, ESLint 10 | TypeScript 5.9.3, ESLint 9.39.5 | `openapi-typescript` 7.13 peers on `typescript ^5`; `eslint-plugin-react`/`import`/`jsx-a11y` peer on `eslint ^9` |
| HeroUI's React Aria peers installed implicitly by npm | Pinned explicitly in `package.json` | HeroUI's quick-start says to install missing mandatory peers; "pin every dependency" |

**What was checked rather than assumed:** the `"use client"` question was answered by grepping `node_modules/@heroui/react/dist` (every component implementation file starts with `"use client"`; barrels, hooks and utils do not). HeroUI setup came from the MCP quick-start and frameworks guide; Next.js config from the bundled `node_modules/next/dist/docs/`.

**Checklist items triggered:** [workflow.md §2](workflow.md): outdated APIs (MSW 3 rename, Next 16 generator defaults), security (`react/no-danger` as an error, `poweredByHeader: false`, no module-level store).

---

## #007 — US01 with a lead session driving the agents (tests first, then code)

- **Date:** 2026-10-20 → 21
- **Tool:** Claude Code (Claude Opus 5.5): a lead session launching backend-engineer and test-engineer subagents in separate git worktrees
- **Phase:** P1–P4, story by story

**Prompt (summary):** Instead of the human copy-pasting prompts between sessions, the lead session launched the agents itself: the test engineer wrote failing acceptance ITs for `GET /api/v1/pokemon` from the contract only (real recorded PokeAPI fixtures, WireMock, RestAssured); in parallel the backend engineer wrote failing domain tests and **stopped** for human review; after approval it implemented inside-out until everything was green, in one PR.

**Output:** PR #17 (US01: 27 acceptance cases, ~300 unit tests, PIT 100%), then #24 (springdoc contract check, 1 MB upstream limit, problem types).

**Verdict:** ✏️ Modified

| What the AI did or proposed | What was changed / decided | Why |
|---|---|---|
| Shared test harness: `@DynamicPropertySource` + a per-class `WireMockExtension` | One JVM-wide WireMock server, reset + cache clear before each test | Review found the two IT classes shared one Spring context (same cache key) while WireMock restarted on a new port: tests could pass for the wrong reason (connection refused → 503) |
| `.cookie("JSESSIONID", nullValue())` to assert "no session" | Assert on the cookie map and every `Set-Cookie` header | RestAssured fails that matcher when the cookie is **absent**, the exact case being tested; the backend agent refused to edit a test it didn't own and reported it |
| Placeholder sprite: undefined | A real allow-listed PokeAPI sprite (poke-ball) | Passes the strict `ImageUrl` rules without a bypass |

**What I checked critically:** the agents' claims were verified against `git diff` (developers never touched the tester's `*IT` files) and CI logs, not taken from the handoff notes.

---

## #008 — Overnight push: US02, Auth and Collection under a standing approval

- **Date:** 2026-10-20 23:00 → 2026-10-21 11:00
- **Tool:** Claude Code (Claude Opus 5.5): lead session + up to 3 backend engineers and 1 test engineer in parallel worktrees
- **Phase:** P1–P4

**Prompt (summary):** Deadline: all 12 endpoints by 14:00. The human gave a **standing approval** with explicit stop-rules (stop only for a breaking contract change, a new unmanaged dependency, weakening a security rule or gate, or a new ADR). The lead reviewed every red test before its implementation started.

**Output:** PRs #25 (US02 detail, evolution tree), #26 (auth: register/login/logout/me/csrf), #27 (collection: sync/list/get/update/delete). 202 ITs green at the end.

**Verdict:** ✏️ Modified

| What the AI did or proposed | What was changed / decided | Why |
|---|---|---|
| `oauth2ResourceServer(o -> o.jwt())` (the usual AI default) | Hand-wired `BearerTokenAuthenticationFilter` reading the cookie only | The DSL silently adds `csrf.ignoringRequestMatchers(<bearer requests>)`; with a cookie token **CSRF was off for every logged-in write**. The test engineer's `LogoutIT` caught it (204 instead of 403) |
| Password policy: 12–64 characters | Also ≤ 72 UTF-8 bytes | BCrypt ignores bytes after 72; 64 emoji are up to 256 bytes |
| Gitleaks failed on fake test values | A `.gitleaksignore` with **exact fingerprints**, never by path | The first version quoted the values in its comments and was flagged itself |
| A 67-case collection suite and a 33-case detail suite | New standing rule: **lean acceptance tests** (happy path + contract statuses + documented edge cases; boundaries in unit tests) | The human found the suites slowed delivery |

**What I checked critically:** usage limits interrupted the agents twice; work resumed from git state, never from memory. A squash-merged dependency branch made a PR "conflict": verified `main`'s tree equalled the merged branch tip before resolving with the branch's side.

---

## #009 — Frontend Phase A and B with human preview before each PR

- **Date:** 2026-10-21
- **Tool:** Claude Code (Claude Opus 5.5): frontend-engineer subagent; lead built Docker previews and drove the in-app browser
- **Phase:** P5

**Prompt (summary):** Phase A: app shell, theme, catalog and detail pages (Server Components). Phase B: auth pages, RTK Query layer with CSRF handling, collection table, sync, edit form with conflict dialog. Both pushed **without a PR** until the human had used a running preview.

**Output:** PRs #29 (Phase A) and #34 (Phase B), 112 Vitest tests.

**Verdict:** ✏️ Modified

| What the AI did or proposed | What was changed / decided | Why |
|---|---|---|
| Footer "Data from PokeAPI · v1.0" | "Pokémon data from PokéAPI v2 (pokeapi.co)" | The human read "v1.0" as the PokeAPI version |
| Assumed the Next.js `/api` rewrite forwards `X-Forwarded-For` | Echo-server test: it forwards nothing, and passes a client-supplied value unchanged | Led to the nginx edge proxy (#35, ADR 0011) |
| Optimistic redirect checks `access_token` (security.md §3.3) | `pokedex_signed_in` hint cookie (ADR 0012) | `access_token` is `Path=/api`, invisible to page routes |
| `upgrade-insecure-requests` + HSTS always on | Only over HTTPS | Broke `/_next` assets on `http://localhost` in Docker |
| HeroUI `Meter.Fill` / next/image inline styles | SVG width / classes | The nonce-based CSP blocks inline `style` attributes in server-rendered HTML |

---

## #010 — Running it: Docker Compose, Postman, least-privilege DB, nginx, demo seed, offline mode

- **Date:** 2026-10-21 → 22
- **Tool:** Claude Code (Claude Opus 5.5): lead session (Compose, Postman, previews) + backend/test engineers
- **Phase:** P6

**Prompt (summary):** "I need to test the UI and backend manually": full-stack Compose, a Postman collection, then the hardening and demo features that surfaced while testing.

**Output:** #28 (Dockerfiles + Compose + README), Postman collection (27 requests, cookies/CSRF automated, run with Newman: 0 failures), #30 (least-privilege `pokedex_app` role), #35 (nginx edge), #36 (demo seed + `/actuator/health`), #37 (offline WireMock PokeAPI, Gen 1).

**Verdict:** ✏️ Modified

| What the AI did or proposed | What was changed / decided | Why |
|---|---|---|
| Health check body `{"groups":["liveness","readiness"],"status":"UP"}` | Probes off: exactly `{"status":"UP"}` | Public endpoint must be status only (security.md §3.2); the matrix IT caught it |
| Demo seed always on in the `demo` profile | Runs only when a `DEMO_*` variable is set | The contract IT uses `demo` and would have called live PokeAPI and written into the shared IT database |
| "Offline" as a Compose profile | A Compose override file | A profile can't change the backend's env or startup order (ADR 0014) |
| The human's stack failed after #30 | `docker compose down -v` once | The init script that creates the app role only runs on an empty volume |

---

## #011 — The dependency scan that took 1 h 35 min and found real CVEs

- **Date:** 2026-10-20 → 21
- **Tool:** Claude Code (Claude Opus 5.5): lead session + test engineer
- **Phase:** P0 / CI

**Summary:** OWASP Dependency-Check needs the full NVD database (≈ 412k records). The first CI runs timed out (45 min, then the cache was only saved on success), later downloads stalled, and caches saved by a PR are invisible to other PRs. When it finally completed it found **9 CVEs (CVSS 7.5–9.8) in Tomcat 11.0.24**, fixed with a version override (#10).

**Decisions:** the scan runs weekly and on demand only (#18); the cache is saved with `if: always()`; Dependabot PRs need their own copy of the `NVD_API_KEY` secret; a stale `gh run rerun` re-tests the old commit, so after merging a fix the PR branch must be updated instead.

**What I checked critically:** an "Invalid API key" error the human saw was from an old log (timestamp before the key change); the newer runs had accepted the key.

---

## #012 — Catalog redesign from design critiques, and name search (autocomplete)

- **Date:** 2026-10-22
- **Tool:** Claude Code (Claude Opus 5.5): `/design:design-critique` on the human's screenshots; lead session; backend, frontend and test engineers; Docker previews in the in-app browser
- **Phase:** P5 polish

**Prompt (summary):** The human ran design critiques on the catalog and on the detail page's Evolution panel, then asked for the fixes, choosing a **partial autocomplete** search over an exact-match jump ("do the partial autocomplete, what makes better").

**Output:** PR #45 (backend: `GET /api/v1/pokemon/search`, `types` on list items; 244 ITs, PIT 100%) and the frontend redesign PR (crisp sprites, autocomplete, type chips, top pager, labels, detail layout).

**Verdict:** ✏️ Modified (two layout iterations rejected by the human)

| What the AI proposed | What the human decided / what changed | Why |
|---|---|---|
| Exact name/number jump (frontend only) as the default for "Find a Pokémon" | **Partial autocomplete** with a new backend endpoint | Better UX; PokeAPI has no search, so the backend loads all 1,351 names once (cached 24 h) and filters in memory |
| Evolution as its own **full-width row** under stats | **Rejected:** "it's taking so much space" | — |
| Evolution side by side with Base stats in a bottom row | **Rejected:** "I have to scroll to get the information of the evolution" | — |
| — | **Evolution in the right column, directly under About**; compact 112 px cards with official artwork; "This Pokémon" badge + border kept | Fills the empty space beside the stats; visible without scrolling at 1206 × 858 |
| Pixel sprites for evolution stages | Official artwork | Same art style as the hero image; equal sizes (Bulbasaur's sprite has a lot of empty space) |
| "Mass" on the detail page | "Weight" | Consistency with the catalog cards |

**What was checked:** every iteration was built into a Docker preview and checked in the browser before any PR; the E2E console guard caught an inline SVG that violated the CSP (fixed with a text arrow); the test engineer caught that the offline mock couldn't answer the "all names" request (fixed in the mock).

---

## Template

```markdown
## #NNN — <short title>

- **Date:** YYYY-MM-DD
- **Tool:** <tool + model>
- **Phase:** <P0–P7>

**Prompt:** <exact prompt or a faithful summary>

**Output:** <what was generated — files/classes, or a short sample>

**Verdict:** ✅ / ✏️ / ❌

**What I checked / changed:**
- <boundary leaks, wrong units, outdated APIs, missing edge cases, …>

**Checklist items triggered:** <link to workflow.md sections>
```
