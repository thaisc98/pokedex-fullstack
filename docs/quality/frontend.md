# Frontend (Next.js 16 + HeroUI v3 + RTK Query): Verification Report

> Mode B · Test engineer · `origin/main` HEAD `0fa2732` (PR #37) · 2026-10-22
> Commands run in `frontend/` with Node from `.nvmrc`.

## Verdict

**Static gates and unit tests: PASS.** **UX acceptance (ui-ux.md §11–12): pending.** Playwright, axe, the console guard and the 3 viewports are being built on `agent/test/e2e-playwright` and are not on main yet. Unit coverage is good on the logic modules (`shared/lib`, `shared/api`, catalog and edit-form logic). It is **29% of all source**, because components and pages are left to E2E.

## Scope

| Item | Value |
|---|---|
| Routes | `/`, `/pokemon`, `/pokemon/[idOrName]`, `/login`, `/register`, `/collection`, `/collection/[pokedexId]`, `_not-found`, `proxy.ts` |
| PRs | #19 (P0 scaffold), #20 (frontend CI + `npm audit`), #21/#23 (deps), #29 (Phase A: shell, catalog, detail), #34 (Phase B: auth, collection, sync, edit), #28/#37 (compose, offline) |

## Gates

| Gate | Command | Result |
|---|---|---|
| Install | `npm ci` | ✅ |
| Lint | `npm run lint` (ESLint 10, `eslint-config-next` 16.4.2) | ✅ 0 errors, 0 warnings |
| Types | `npm run typecheck` (`next typegen && tsc --noEmit`, strict) | ✅ |
| Unit / component | `npm test` (Vitest 5.0.5, jsdom, MSW 3) | ✅ **112 tests, 11 files, 0 failures** (5.5 s) |
| Coverage | `npm test -- --coverage` | ❌ **does not run**: `MISSING DEPENDENCY @vitest/coverage-v8` (see F-FE-1). Numbers below come from a temporary `npm i --no-save @vitest/coverage-v8@5.0.5`; nothing committed. |
| Build | `npm run build` (Next.js 16.4.2, Turbopack) | ✅ 8 dynamic routes + Proxy, 0 warnings |
| Audit (prod) | `npm audit --omit=dev --audit-level=high` | ✅ 0 vulnerabilities |
| Audit (all) | `npm audit` | 🟡 5 high, all **dev-only**: `braces` via `micromatch` → `fast-glob` → `@next/eslint-plugin-next` → `eslint-config-next` |
| E2E 375/768/1280, axe, console guard | `npm run test:e2e` | ⏳ pending (no `frontend/e2e/` on main) |
| Lighthouse CI | — | ⏳ not configured |

### Vitest per file

| File | Tests |
|---|---|
| `shared/lib/formatName.test.ts` | 17 |
| `shared/lib/isSafeHttpUrl.test.ts` | 17 |
| `features/collection/lib/editForm.test.ts` | 18 |
| `features/catalog/lib/catalogParams.test.ts` | 15 |
| `shared/lib/safeRedirect.test.ts` | 14 |
| `features/catalog/components/EvolutionTree.test.tsx` | 10 |
| `shared/api/baseQuery.test.ts` | 6 |
| `shared/lib/problemCopy.test.ts` | 6 |
| `store/StoreProvider.test.tsx` | 4 |
| `features/auth/SessionBootstrap.test.tsx` | 3 |
| `features/catalog/components/TypeBadge.test.tsx` | 2 |

### Coverage (v8)

| Scope | Statements | Branches | Functions | Lines |
|---|---|---|---|---|
| Files loaded by tests (Vitest default) | 85.23% (277/325) | 79.23% (206/260) | 77.77% (70/90) | **86.95%** (240/276) |
| **All of `src/**`** (excl. tests, `.d.ts`, `mocks/`) | 30.42% (275/904) | 30.42% (206/677) | 25.64% (70/273) | **29.38%** (238/810) |

Well covered (≥ 90% lines): `baseQuery.ts` 97%, `isSafeHttpUrl.ts`, `problemCopy.ts`, `editForm.ts`, `catalogParams.ts`, `evolution.ts`, `EvolutionTree.tsx`, `TypeBadge.tsx`, `SessionBootstrap.tsx`, `makeStore.ts` (all 100%), `safeRedirect.ts` 90%.
Partly covered: `authApi.ts` 35%, `apiError.ts` 54%, `collectionApi.ts` 56% (0% branch), `cookies.ts` 73% (29% branch).
0%: every page and layout, `proxy.ts`, `server/catalogClient.ts`, `LoginForm`, `RegisterForm`, `RequireAuth`, `UserMenu`, `formErrors.ts`, `AddToCollectionButton`, `CatalogPagination`, `EditForm`, `ErrorSummary`, `DeleteDialog`, `CollectionList`, `CollectedDetail`, `RowActions`, `collectionParams.ts`, `MainNav`, `ErrorState`, `EmptyState`.

## Acceptance criteria → tests

| Criterion (source) | Proven by | Result |
|---|---|---|
| CSRF: non-GET sends `X-XSRF-TOKEN`, read fresh each time; fetch `/auth/csrf` when there is no cookie (§5 Frontend) | `baseQuery.test.ts.should_send_the_current_xsrf_cookie_on_writes_and_read_it_fresh_each_time`, `…should_fetch_a_csrf_token_first_when_there_is_no_cookie` | ✅ |
| 403 `csrf-token-invalid` → new token, retry **once**; other 403s not retried (§5) | `…should_retry_once_with_a_new_token_after_a_csrf_403`, `…should_not_retry_a_403_that_is_not_about_csrf` | ✅ |
| 401 mid-session → sign out (§5) | `…should_end_the_session_on_401` | ✅ (redirect to login and back ⏳ E2E) |
| 429 exposes `Retry-After` | `…should_expose_retry_after_on_429` | ✅ |
| `isSafeHttpUrl`, `from` redirect check (security §6) | `isSafeHttpUrl.test.ts` (17), `safeRedirect.test.ts` (14) | ✅ |
| Store per request (`makeStore()` per `StoreProvider`), stable across re-renders | `StoreProvider.test.tsx` (4) | ✅ |
| Every contract problem type has friendly copy | `problemCopy.test.ts.should_cover_every_problem_type_in_the_contract` | ✅ |
| Page number lives in the URL (back/forward) | `catalogParams.test.ts` (15) | ✅ unit / ⏳ E2E |
| Edit-form rules mirror the API (lat 91, 10 tags, one name per language, language code, tag charset) | `editForm.test.ts` (18) | ✅ |
| Type is shown as text, not only as color (WCAG) | `TypeBadge.test.tsx.should_show_the_type_as_a_text_label` | ✅ |
| Double submit blocked (button disabled while pending) (§5) | Code: `isPending`/`isDisabled` on `AddToCollectionButton` and `EditForm`; no test | ⏳ pending |
| Loading / empty / error-with-retry / image placeholder (ui-ux §7) | `loading.tsx`, `error.tsx`, `EmptyState`, `ErrorState`, `Sprite` exist; no test | ⏳ pending |
| ui-ux §11 journeys (US01–US04, Auth), 0 console warnings, 0 serious/critical axe, 3 viewports | Playwright | ⏳ pending |

## Security checks

| Check | Evidence | Result |
|---|---|---|
| No `dangerouslySetInnerHTML`, `localStorage`, `sessionStorage`, `NEXT_PUBLIC_*`, raw `<a href>` in `src/` | `grep`: 0 hits (one doc comment in `AppLink.tsx`) | ✅ |
| Server-only code imports `'server-only'` | `server/catalogClient.ts` | ✅ |
| Page CSP with a per-request nonce + §4.7 headers | `proxy.ts` lines 57–82 (review only; 0% test coverage) | 🟡 reviewed, untested; E2E console guard will catch CSP violations |
| `npm audit` (prod) in CI | `frontend-audit` job, green on main (run 39780505513) | ✅ |

## Known gaps and accepted risks

| Item | Status |
|---|---|
| Playwright E2E (3 viewports, console guard, axe) | **Pending**, in progress on `agent/test/e2e-playwright` |
| Not-found pages return HTTP 200 (streamed after `loading.tsx`) | Accepted risk |
| Sprites don't load offline (GitHub-hosted) | Accepted; placeholder shown |
| `braces` high advisory, dev-only (ESLint toolchain); the fix is a breaking downgrade of `eslint-config-next` | Accepted; CI audits `--omit=dev` |
| Lighthouse CI, ZAP baseline | Not run yet |

## Findings

| ID | Severity | Finding | Suggestion |
|---|---|---|---|
| F-FE-1 | Medium | `npm test -- --coverage` fails: `@vitest/coverage-v8` is not a devDependency, and `vitest.config.mts` has no `coverage` block. Frontend coverage can't be measured in CI, and testing-strategy has no frontend coverage gate to enforce. | Frontend engineer: add `@vitest/coverage-v8` (pinned to 5.0.5) and a `coverage.include: ["src/**"]` so untested files count. |
| F-FE-2 | Medium | Vitest's default report shows **87% lines**. That counts only files the tests import. Over all source it is **29%**, and all 6 interactive forms/dialogs (`LoginForm`, `RegisterForm`, `EditForm` + `ErrorSummary` focus, `DeleteDialog`, `AddToCollectionButton`) have no component test. Until Playwright lands, nothing automated checks the §11 behaviours: error-summary focus, conflict dialog, double-submit block, USER-hides-Delete. | Land the Playwright suite. Consider RTL tests for `EditForm` (focus on the error summary, 409 dialog) and `RowActions` (role-based Delete). |
| F-FE-3 | Low | `proxy.ts` (CSP nonce, security headers, optimistic redirect) and `catalogClient.ts` (server fetch, error mapping) are untested. | E2E header assertions + a Vitest unit for `catalogClient` error mapping with MSW. |
| F-FE-4 | Low | `collectionApi.ts` has 0% branch coverage (cache-tag invalidation after sync/update/delete untested). | MSW test: after a successful `PUT`, the list query refetches. |
| F-FE-5 | Low | `SessionBootstrap.test.tsx.should_not_call_auth_me_for_visitors` proves "no `/auth/me` call" only **indirectly**. It relies on MSW `onUnhandledFrame: "error"` (`vitest.setup.ts`), which turns an unexpected request into a network error and a console error but does **not** fail the test itself. The test then asserts only `checked === true` and `user === null`, and both still hold if the component wrongly probes **and** dispatches `sessionEnded()`. The other 111 tests assert concrete values, DOM roles/attributes or store state. No hollow assertions there. | Record requests (`server.events.on("request:start", …)`) and assert none hit `/auth/me`, or fail tests on `console.error`. |
