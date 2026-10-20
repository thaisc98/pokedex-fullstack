# P0 Backend Verification Report

> Mode B (verify-after-implementation) · Test engineer
> Branch: `agent/test/p0-verify` (cut from `origin/main`)
> Verified against `origin/main` HEAD `1c62e0f` ("ci: configurable Postgres port, scan only on dependency changes…", PR #11)
> Date: 2026-10-20

This report verifies the **P0 — Inception & Scaffolding** exit criteria from
[`docs/roadmap.md`](../roadmap.md) against the code actually on `origin/main`.
Numbers are from commands run in this worktree's `backend/` dir; CI facts are
from `gh run`/`gh pr`. Nothing invented — where a thing does not exist yet, it is
stated as such.

---

## Overall verdict

**P0 backend: PASS (with one caveat on CI timing).**

- `./mvnw verify` → **BUILD SUCCESS**; all guards (enforcer, ArchUnit, Spotless) green.
- Mutation profile → **BUILD SUCCESS**, scores well above the 75% gate.
- Enforcer-ban and ArchUnit cross-layer guards → **proven to fail for the right reason** in PR #7.
- Backend `./mvnw verify` **passes in CI on main** (run 39513680312, 49s).
- The OWASP Dependency-Check `-Psecurity` profile and the Tomcat CVE override are
  **merged to main** (PRs #7 and #10) — not open PRs. The dependency-scan CI job
  (PRs #8/#11) exists and runs on main, but as of this report its **first** run was
  still **in progress** (one-time full NVD database download); no fully-green
  dependency-scan run has completed yet. Caveat, not a failure.

Note on the task brief: it anticipated PRs #7/#10 being *open* and the backend/
dependency-scan CI jobs possibly *absent*. Reality on `origin/main` is further
along — #7 (`8664cbc`), #10 (`a521c19`), #8 (`ca62a02`) and #11 (`1c62e0f`) are
all **merged**. This report describes the merged state.

---

## 1. Pass/fail per P0 exit criterion

### Criterion: `./mvnw verify` … pass (locally + in CI)

**Local `./mvnw verify` → BUILD SUCCESS** (Java 25.0.4.1, Spring Boot 4.1.1, total 7.6s).

Per-module test counts (Surefire = `*Test`, Failsafe = `*IT`):

| Module | Surefire (`*Test`) | Failsafe (`*IT`) | Notes |
|---|---|---|---|
| domain | 3 (`SampleTest`) | 0 | — |
| application | 2 (`SampleServiceTest`) | 0 | uses `@Mock` + the `InMemorySampleGateway` fake |
| infrastructure | 1 (`SampleAdapterTest`) | 0 | — |
| presentation | 1 (`SampleControllerTest`) | 0 | `@WebMvcTest` + `@MockitoBean` |
| bootstrap | 2 (`ArchitectureTest`, 2 `@ArchTest` rules) | 1 (`SampleIT`) | `@SpringBootTest` context-loads |
| **Total** | **9** | **1** | all passed, 0 failures/errors/skips |

JaCoCo line + branch coverage per module (from `*/target/site/jacoco/jacoco.csv`):

| Module | Instr. covered | Branch | Line | Interpretation |
|---|---|---|---|---|
| domain (`Sample`) | 20/20 | 4/4 = **100%** | 5/5 = **100%** | meets P1 gate 95%/90% |
| application (`SampleService`) | 12/12 | 0/0 (n/a) | 4/4 = **100%** | no branches in sample |
| infrastructure (`SampleAdapter`) | 5/5 | 0/0 (n/a) | 2/2 = **100%** | — |
| presentation (`SampleController`,`SampleResponse`) | 19/19 | 0/0 (n/a) | 5/5 = **100%** | — |
| bootstrap | — | — | — | aggregate report generated; `PokedexApplication.main` excluded per strategy §3; no per-module gate at P0 |

These are 100% because the only classes are the trivial P0 scaffolding samples.
Coverage is therefore **not yet meaningful** as a quality signal — the real gates
(domain/application ≥ 95% line, ≥ 90% branch; presentation 85/80; infrastructure
80/75) bite in P1–P4 once real classes exist. For P0 the point is only that JaCoCo
runs and reports on Java 25 bytecode, which it does.

Guards inside `verify`:
- **Enforcer**: RequireJavaVersion ✅, RequireMavenVersion ✅, DependencyConvergence ✅, `ban-frameworks` BannedDependencies ✅ (domain + application).
- **ArchUnit** (`ArchitectureTest`): both rules green (layering + domain/application framework-free).
- **Spotless** (`spotless:check`): all 16 files clean across modules.

**CI (main):** run **39513680312** ("ci: backend verify + dependency-scan jobs…",
push to main) — **Backend (./mvnw verify) ✓ in 49s**, Secret scan ✓ in 9s.
→ <https://github.com/thaisc98/pokedex-fullstack/actions/runs/39513680312>
This satisfies the roadmap line "`./mvnw verify` … pass in CI" for the backend.
(The `npm run build` half of that line is frontend — see §4.)

### Criterion: adding `spring-context` to `domain` fails the build (enforcer)

**Proven in PR #7** (merged, `8664cbc`). On a throwaway branch the backend agent
added `spring-context` to `domain/pom.xml` and ran `./mvnw verify`; the enforcer
`ban-frameworks` rule failed:

```
[ERROR] Rule 0: org.apache.maven.enforcer.rules.dependency.BannedDependencies failed with message:
[ERROR] domain and application must stay free of Spring, JPA, Jackson and Lombok (ADR 0001).
[ERROR]    org.springframework:spring-context:jar:7.0.9 <--- banned via the exclude/include list
```

Attributed to PR #7 (<https://github.com/thaisc98/pokedex-fullstack/pull/7>); not
re-run here (destructive). The rule is present and green on `origin/main` today
(see §1 enforcer result above). **PASS.**

### Criterion: a deliberate cross-layer import fails ArchUnit

**Proven in PR #7** (merged). Infrastructure `SampleAdapter` was made to import
presentation `SampleResponse`; `ArchitectureTest` failed:

```
Architecture Violation [Priority: MEDIUM] - Rule 'Layered architecture ...
where layer 'Presentation' may only be accessed by layers ['Bootstrap']' was violated (2 times):
Method <com.pokedex.infrastructure.shared.SampleAdapter.load()> calls constructor
  <com.pokedex.presentation.shared.SampleResponse.<init>(int)> in (SampleAdapter.java:15)
[ERROR] Tests run: 2, Failures: 1 -- BUILD FAILURE
```

Attributed to PR #7; not re-run here. The `ArchitectureTest` is present and green
on `origin/main` today. **PASS.**

### Criterion: a sample test runs under JaCoCo, PIT, ArchUnit and Mockito on Java 25 bytecode

All confirmed in the local run:
- **JaCoCo**: agent attached, per-module + aggregate reports generated (§1 table).
- **ArchUnit**: `ArchitectureTest` printed `Detected Java version 25.0.4.1` and passed.
- **Mockito (ByteBuddy)**: `SampleServiceTest` and `SampleControllerTest` (`@MockitoBean`)
  pass. The log shows the known Mockito self-attach / dynamic-agent warnings on JDK 25
  — informational only, build still green. *(See Findings — not a P0 blocker.)*
- **PIT**: `./mvnw -pl domain,application -Pmutation verify` → **BUILD SUCCESS**:

| Module | Mutations generated | Killed | Mutation score | Line cov. (mutated classes) | Test strength |
|---|---|---|---|---|---|
| domain (`Sample`) | 5 | 5 | **100%** | 5/6 = 83% | 100% |
| application (`SampleService`) | 2 | 2 | **100%** | 4/4 = 100% | 100% |

Both are above the ≥ 75% gate. (The "5/6 = 83%" in PR #7's description is PIT's
*line coverage of mutated classes*, a different number from the *mutation score*;
this fresh run killed every generated mutant.) **PASS.**

### Criterion: Dependency-Check and `npm audit` run in CI

Backend half — **implemented and merged** (not "open PR" as the brief assumed):
- PR #7 (`8664cbc`, merged) added the `-Psecurity` Maven profile to
  `backend/pom.xml` with CycloneDX `2.9.3` (aggregate SBOM) and OWASP
  `dependency-check-maven 13.0.0` (`failBuildOnCVSS=7`). Confirmed present on
  `origin/main` (`<id>security</id>` at `backend/pom.xml:229`).
- PR #10 (`a521c19`, merged) added the Tomcat `11.0.27` CVE override
  (`<tomcat.version>11.0.27</tomcat.version>` at `backend/pom.xml:56`).
- PRs #8 (`ca62a02`) and #11 (`1c62e0f`), merged, added the `dependency-scan` CI
  job to `.github/workflows/ci.yml` (runs `./mvnw -Psecurity -DskipTests verify`,
  with a fail-closed guard that errors if the `security` profile is missing).

CI status, honestly: the **dependency-scan job has not completed a green run yet.**
On run **39513680312** it was still **in progress** (`* Dependency scan (OWASP
Dependency-Check)`) at report time — this is the one-time full NVD database
download (job `timeout-minutes: 120`). A newer run **39514786002** on HEAD
`1c62e0f` was **pending**. So this criterion is **"implemented and merged, first
CI scan pending completion"** — I cannot assert a green dependency-scan run
because none has finished.

`npm audit` half — **not present on main yet.** `ci.yml` has an explicit
`TODO(Phase 0, frontend)` block for `frontend` and `frontend-audit` jobs; they are
commented out because no `frontend/package.json` exists. See §4. This sub-criterion
is **not done** (correctly deferred with the rest of the frontend).

**Summary for this criterion:** backend Dependency-Check = implemented/merged,
green CI run pending; `npm audit` = not present yet. Partial.

### P0 criteria scoreboard

| Exit criterion | Status |
|---|---|
| `./mvnw verify` passes (local + CI) | ✅ PASS (CI run 39513680312, 49s) |
| `npm run build` passes in CI | ⏭️ N/A at P0 — no frontend on main (§4) |
| `spring-context` in `domain` fails the build (enforcer) | ✅ PASS (proven PR #7) |
| Deliberate cross-layer import fails ArchUnit | ✅ PASS (proven PR #7) |
| Sample test runs under JaCoCo/PIT/ArchUnit/Mockito on Java 25 | ✅ PASS |
| Dependency-Check runs in CI | 🟡 implemented+merged; first NVD scan still in progress — no green run observed |
| `npm audit` runs in CI | ⏭️ not present yet (frontend deferred) |

---

## 2. Contract vs docs

Compared `docs/api/openapi.yaml` against `docs/architecture.md` §2.8 (endpoint
table) and `docs/security.md` §3.2 (authorization matrix). Server base path in the
contract is `/api/v1` (`servers[0].url`), so e.g. contract `/pokemon` = §2.8
`/api/v1/pokemon`. Auth modelled as `accessTokenCookie` (apiKey, cookie
`access_token`) + a `CsrfHeader` (`X-XSRF-TOKEN`) parameter on writes.

| Endpoint (method) | §2.8 success / errors | security.md §3.2 auth | openapi.yaml | Consistent? |
|---|---|---|---|---|
| GET `/pokemon` | 200 / 400, 503 | ANON/USER/ADMIN ✅ | 200,400,503; `security: []` | ✅ |
| GET `/pokemon/{idOrName}` | 200 / 400,404,503 | public | 200,400,404,503; `security: []` | ✅ |
| POST `/auth/register` | 201 / 400,403(CSRF),409,413,415,429 | public | 201,400,403,409,413,415,429 | ✅ |
| GET `/auth/csrf` | 200 / — | public | 200; `security: []` | ✅ |
| POST `/auth/login` | 200 / 400,401,403(CSRF),413,415,429 | public | 200,400,401,403,413,415,429 | ✅ |
| POST `/auth/logout` | 204 / 401,403(CSRF) | USER (401 anon) | 204,401,403; cookie auth | ✅ |
| GET `/auth/me` | 200 / 401 | USER (401 anon) | 200,401; cookie auth | ✅ |
| POST `/collection` | 201/200 / 400,401,403(CSRF),404,413,415,503 | USER (401 anon) | 201,200,400,401,403,404,413,415,503 | ✅ |
| GET `/collection` | 200 / 400,401 | USER (401 anon) | 200,400,401; cookie auth | ✅ |
| GET `/collection/{pokedexId}` | 200 / 401,404 | USER (401 anon) | 200,401,404 | ✅ |
| PUT `/collection/{pokedexId}` | 200 / 400,401,403(CSRF),404,409,413,415 | USER (401 anon) | 200,400,401,403,404,409,413,415 | ✅ |
| DELETE `/collection/{pokedexId}` | 204 / 401,403(CSRF or not ADMIN),404 | ADMIN (401 anon, **403** USER) | 204,401,403,404; 403 description covers both reasons | ✅ |

**Known, intended divergences (not defects):**
- 403 (CSRF), 413, 415 appear throughout the contract on write endpoints but are
  *not literally* in the §2.8 success/error columns for every row — they come from
  security.md §4.2.1 (CSRF) and §4.3 (16 KB → 413, non-JSON → 415). §2.8 itself
  calls this out: "Every endpoint with a request body also returns 413 … and 415".
  The contract makes them explicit per-endpoint, which is *more* faithful, not less.
- Path variable naming: §3.2 (security.md) writes `{id}` for PUT/DELETE, while §2.8
  and the contract use `{pokedexId}`. Cosmetic; §2.8 and the OpenAPI agree, and §2.8
  states "if they disagree, the YAML wins". No action needed.
- The 403 on DELETE is documented in the contract as covering **both** CSRF-invalid
  and not-ADMIN, matching §3.2's "**403**" for USER and the §2.8 "403 (CSRF or not
  ADMIN)" note. Correct.

**Verdict: the contract faithfully represents the documented API.** Every endpoint's
path, method, auth and success+error codes are consistent with §2.8 and §3.2; the
only deltas are the deliberate CSRF/413/415 additions (sourced from security.md) and
a cosmetic path-param name difference against security.md. No contradictions found.

> Caveat: this is a doc-vs-doc review. No code implements these endpoints at P0, so
> there is no springdoc output to diff against the contract yet. That contract-vs-
> springdoc check (README §2, "/v3/api-docs vs openapi.yaml") is a P4 gate.

---

## 3. Hollow-assertion review of the P0 sample tests

These are **deliberately minimal P0 scaffolding proofs** (prove the toolchain runs
on Java 25). Reviewed read-only; I own none of them. Framing findings as
"acceptable for P0 / replace in P1+" where that applies.

| Test | Assessment | Severity | Suggestion |
|---|---|---|---|
| `SampleTest` (domain) | **Solid.** Three real behavioural assertions (reject negative, positive→true, zero→false). Covers both boundary branches, which is exactly what gives PIT mutants to kill. No hollowness. | — | Keep as the pattern for P1 value-object tests. |
| `SampleServiceTest` (application) | **Mostly solid.** `should_double…` asserts the *value* (`isEqualTo(42)`) **and then** `verify(gateway).load()` — the interaction check is additive, not a substitute for a state/value assertion, so not hollow. `should_work_with_the_in_memory_fake` asserts the computed value via the fake. Good demo of "prefer fakes, use Mockito only to confirm an interaction". | Low | In P2, keep value assertions primary; reserve `verify(...)` for "never called" cases (e.g. "sync does not call save on upstream error"), per testing-strategy §1. |
| `InMemorySampleGateway` (application test-jar) | Not a test — a fake. Trivial by design. Correctly shipped in the test-jar as the pattern for P2 fakes. | — | — |
| `SampleAdapterTest` (infrastructure) | **Borderline-weak but acceptable for P0.** `assertThat(new SampleAdapter().load()).isEqualTo(21)` asserts a hardcoded constant against its own hardcoded return — a near-tautology with no collaborator, no error path, no mapping/translation. It proves Surefire runs in the infra module and nothing more. | Low (P0) / Medium if it survives into P3 | In P3 replace with WireMock-backed `*IT` for the PokeAPI adapter: assert mapping, 404→404, 5xx→retry→503, SSRF URL rebuild, and cache-hit `verify(0)` counts (testing-strategy §4/§5). |
| `SampleControllerTest` (presentation) | **Acceptable.** `@WebMvcTest` + `@MockitoBean`; stubs `service.doubled()→42` then asserts `status().isOk()` and `jsonPath("$.value").value(42)`. Real status + body assertion through the MVC stack — proves the slice wiring. Thin only because the controller is thin. | Low | In P4 this becomes the home for status-code, validation→400, ProblemDetail-shape and 401/403 assertions; add `.with(csrf())` write cases and the security matrix there. |
| `ArchitectureTest` (bootstrap) | **Solid and load-bearing.** Two real `ArchRule`s (inward-only layering + domain/application framework-free). Proven to actually fail on a violation (PR #7, §1). Not hollow. | — | In later phases extend with the rule banning `EntityManager.createNativeQuery` outside an allow-listed adapter (security.md §4.4). |
| `SampleIT` (bootstrap) | **Intentionally assertion-free context-loads IT.** The method body is a comment ("context starting … is the assertion"). This is the classic "no explicit assertion" smell — it passes as long as the Spring context boots. **Acceptable for P0** (its whole job is to prove the Failsafe phase + `@SpringBootTest` run on Java 25), but it must NOT be the template for P4 E2E. | Low (P0) / High if still the only `*IT` at P4 | Replace in P4 with the RestAssured E2E flow (register→login→sync→update→get→delete) plus the parameterized endpoint×role security-matrix `*IT`, each with explicit status/body assertions (roadmap P4, security.md §6). A bare context-loads test should never gate a real feature. |

**No false-green risks found at P0.** The one genuine hollow pattern (`SampleIT`
has no assertion) and the one near-tautology (`SampleAdapterTest`) are both
acceptable as toolchain proofs but are flagged to be replaced by real behavioural
tests in P3/P4. No test weakens or fakes a gate.

---

## 4. Frontend criteria — not in scope yet

There is **no frontend code on `origin/main`** (no `frontend/` dir). Accordingly:

- `npm run build` in CI — **not in scope yet; no frontend code on main.** `ci.yml`
  has a commented `TODO(Phase 0, frontend)` `frontend` job (setup-node, `npm ci`,
  lint, `tsc --noEmit`, test, build) awaiting `frontend/package.json`.
- `npm audit` in CI — **not in scope yet; no frontend code on main.** Same TODO
  block includes a `frontend-audit` job (`npm audit --audit-level=high`).
- Next.js 16 / HeroUI v3 / Redux store skeleton, "confirm `@heroui/react` ships
  `use client`" — **not in scope yet; no frontend code on main.**

Not run, not graded. These move into scope when the frontend scaffolding lands.

---

## Appendix — commands run (`backend/`, this HEAD `1c62e0f`)

- `./mvnw verify` → BUILD SUCCESS (7.6s); 9 Surefire + 1 Failsafe tests, 0 failures.
- `./mvnw -pl domain,application -Pmutation verify` → BUILD SUCCESS; domain 5/5
  killed (100%), application 2/2 killed (100%).
- JaCoCo CSV read from `domain|application|infrastructure|presentation/target/site/jacoco/jacoco.csv`.
- `gh pr view 7 / 10`, `gh run list --branch main`, `gh run view 39513680312 / 39490496038 / 39514786002`.

No code, tests, `openapi.yaml` or workflows were modified — only this report was added.
