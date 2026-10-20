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
