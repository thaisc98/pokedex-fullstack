# Authentication (register, login, logout, me, csrf): Verification Report

> Mode B · Test engineer · `origin/main` HEAD `0fa2732` (PR #37) · 2026-10-22
> This report also holds the **backend build numbers** shared by the US01–US04 reports.

## Verdict

**Backend: PASS.** Every identity edge case in testing-strategy §5 and every security.md §6 check owned by the backend has a passing test. **UI journeys: pending** (Playwright on another branch).

## Scope

| Item | Value |
|---|---|
| Endpoints | `GET /auth/csrf`, `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`, `GET /actuator/health` |
| Pages | `/login`, `/register`, `UserMenu`, `SessionBootstrap`, `RequireAuth`, `proxy.ts` |
| PRs | #26 (auth), #31 (auth hardening: client IP, Flyway user, transactions, CSRF docs), #30 (least-privilege DB role), #35 (nginx edge, X-Forwarded-For), #36 (demo seed + `/actuator/health`), #34 (auth pages) |

## Build numbers (shared by every backend report)

`./mvnw -B verify` → **BUILD SUCCESS** (4 min 03 s). Enforcer, ArchUnit, Spotless and the JaCoCo gates all passed. **Flakiness:** 2 more full re-runs (2 min 01 s, 2 min 11 s) gave identical counts and 0 failures, so 3/3 runs were green.

| Module | Unit tests (`*Test`) | ITs (`*IT`) | Line (module report, unit only) | Branch | Gate (line/branch) |
|---|---|---|---|---|---|
| domain | 601 (40 classes) | — | 419/419 = **100%** | 183/188 = **97.3%** | 95 / 90 ✅ |
| application | 101 (12) | — | 135/135 = **100%** | 26/26 = **100%** | 95 / 90 ✅ |
| infrastructure | 186 (20) | — | 611/622 = **98.2%** | 145/162 = **89.5%** | 80 / 75 ✅ |
| presentation | 181 (23) | — | 462/470 = **98.3%** | 78/80 = **97.5%** | 85 / 80 ✅ |
| bootstrap | 25 (7) | 222 (17 classes) | 108/207 = 52.2% | 37/49 = 75.5% | none |
| **Total** | **1,094** | **222** | **Aggregate (unit + IT, 4 library modules): 1,628/1,646 = 98.9% line, 433/456 = 95.0% branch** | | |

0 failures, 0 errors, 0 skipped.

IT classes: `AuthorizationMatrixIT` 45 · `UpdateCollectedPokemonIT` 29 · `ReadCollectionIT` 28 · `GetPokemonDetailIT` 21 · `ListPokemonIT` 15 · `SyncCollectionIT` 14 · `GetPokemonDetailUpstreamFailureIT` 12 · `ListPokemonUpstreamFailureIT` 12 · `CurrentUserIT` 11 · `RegisterIT` 10 · `LoginIT` 7 · `DeleteCollectedPokemonIT` 5 · `LogoutIT` 4 · `OpenApiContractIT` 4 · `PokeApiResponseSizeLimitIT` 3 · `CredentialsNotLoggedIT` 1 · `SampleIT` 1.

`./mvnw -B -pl domain,application -Pmutation verify` → **BUILD SUCCESS** (threshold 75%):

| Module | Mutants | Killed | Score | Line cov. (mutated classes) | Survivors |
|---|---|---|---|---|---|
| domain | 196 | 191 | **97%** | 408/409 | `PasswordHash.toString`, `AccessToken.toString` (→ `""`); 3 boundary mutants in `LocalizedName` (×2) and `GeoMetadata` |
| application | 37 | 35 | **95%** | 132/132 | `AuthenticateUserCommand.toString`, `RegisterUserCommand.toString` (→ `""`) |

## Acceptance criteria → tests

| Criterion (source) | Proven by | Result |
|---|---|---|
| Register → 201 `UserDto`, no secrets in the body | `RegisterIT.should_register_user_and_return_user_dto_without_secrets` | ✅ |
| Email trimmed and lowercased; duplicate (any case) → 409 | `RegisterIT.should_store_email_trimmed_and_lowercased`, `…should_reject_duplicate_email_ignoring_case_with_conflict` | ✅ |
| Password of 11 or 65 chars, or `password1234` → 400; 12 and 64 accepted (§5) | `RegisterIT.should_reject_weak_password_with_validation_problem`, `…should_accept_passwords_of_exactly_12_and_64_characters`; `PasswordPolicyTest` (25) | ✅ |
| Unknown field (`role`) → 400 (mass assignment) | `RegisterIT.should_reject_unknown_fields_such_as_role_with_bad_request` | ✅ |
| Login sets `access_token` with exact attributes (HttpOnly, Secure, SameSite=Strict, Path, Max-Age) | `LoginIT.should_log_in_and_set_access_token_cookie_with_exact_attributes` | ✅ |
| Email in another case logs in | `LoginIT.should_log_in_with_email_in_different_case` | ✅ |
| Wrong password and unknown email → **identical** 401 body (§5) | `LoginIT.should_return_identical_401_for_wrong_password_and_unknown_email`; timing-safe path in `AuthenticateUserServiceTest` (16) | ✅ |
| 6th login in a minute (same IP + email) → 429 + `Retry-After` (§5) | `LoginIT.should_return_429_with_retry_after_on_sixth_login_attempt_within_a_minute_for_one_ip_and_email`; `RateLimiterTest` (7) | ✅ |
| 4th registration in a minute from one IP → 429 + `Retry-After` | `RegisterIT.should_return_429_with_retry_after_on_fourth_registration_within_a_minute_from_one_ip` | ✅ |
| `/auth/me` with cookie → 200; without → 401 | `CurrentUserIT` (2) | ✅ |
| Tampered / garbage signature / expired / `alg:none` / other secret / wrong `aud` / wrong `iss` → 401 (§5, security §6) | `CurrentUserIT` (7 `should_reject_*`) | ✅ |
| Token carries the documented claims | `CurrentUserIT.should_issue_token_with_the_documented_claims`; `JwtTokenIssuerTest` (8) | ✅ |
| Logout → 204 and clears the cookie; anonymous logout → 401 | `LogoutIT` (2) | ✅ |
| Passwords and tokens never appear in logs (§5, security §6) | `CredentialsNotLoggedIT.should_never_log_passwords_or_tokens` (captures output over register → bad login → login → me → logout; checks password, wrong password, token, token signature and JWT secret) | ✅ |
| JWT secret ≥ 256 bits, checked at startup | `JwtSecretKeyTest` (6) | ✅ |
| Demo seed creates ADMIN + USER | `DemoAccountSeederTest` (6) | ✅ |
| UI: signed-out "Add to collection" → login → back to the same Pokémon (ui-ux §11) | Playwright; `safeRedirect.test.ts` (14) covers the `from` check | ⏳ pending (unit part ✅) |
| UI: session restored on reload; visitors don't probe `/auth/me` | `SessionBootstrap.test.tsx` (3) | ✅ |
| UI: 401 mid-session → sign-out → login → return (§5 Frontend) | `baseQuery.test.ts.should_end_the_session_on_401` (sign-out part only) | 🟡 redirect + return is pending E2E |

## Security checks

| Check | Evidence | Result |
|---|---|---|
| Authorization matrix: anonymous / USER / ADMIN for every auth endpoint + actuator | `AuthorizationMatrixIT` (45), `AuthorizationMatrixTest` (18), `ActuatorAccessTest` (5). Completeness: `should_have_a_matrix_row_for_every_served_endpoint` fails when a served endpoint has no row | ✅ |
| Deny by default: unknown paths → 401 anonymous / 403 USER; springdoc closed outside `demo` | `AuthorizationMatrixIT.should_deny_anonymous_get_with_unauthorized_problem`, `…should_deny_user_anything_not_listed_with_forbidden_problem`; `ApiDocsDeniedByDefaultTest` (4) | ✅ |
| `/actuator/health` returns status only; every other actuator path → 401/403 | `AuthorizationMatrixIT` (3 tests); `HealthEndpointTest` | ✅ |
| CSRF: no token → 403 `csrf-token-invalid` on every write | `AuthorizationMatrixIT.should_reject_write_without_csrf_token_with_csrf_problem` (7 rows); `RegisterIT`/`LoginIT`/`LogoutIT` `…without_csrf_token` | ✅ |
| CSRF: `XSRF-TOKEN` cookie readable (not HttpOnly); login and logout **reset** it (old token rejected) | `LoginIT.should_issue_readable_xsrf_cookie_and_token_descriptor`, `…should_reset_csrf_token_on_login_so_the_old_token_is_rejected`, `LogoutIT.should_reset_csrf_token_on_logout_so_the_old_token_is_rejected` | ✅ |
| No `JSESSIONID`, stateless | `StatelessSessionTest` (2), `ListPokemonIT` | ✅ |
| 16 KB request-body limit → 413 on auth bodies | `AuthRequestBodyLimitTest.should_refuse_an_oversized_auth_body_with_413`; `RequestBodyLimitFilterTest` (6: exactly 16 KB passes, declared/undeclared over-limit refused) | ✅ |
| X-Forwarded-For only trusted from the proxy; per-client rate-limit buckets | `ServerHardeningTest.should_give_each_forwarded_client_its_own_registration_bucket`, `…should_ignore_a_spoofed_x_forwarded_for` | ✅ (see F-AUTH-2) |
| nginx edge **overwrites** X-Forwarded-For with `$remote_addr` | `docker/nginx/default.conf` line 19 (config review; no automated test) | 🟡 reviewed, not tested |
| Response compression off (BREACH) | `ServerHardeningTest.should_never_compress_responses` | ✅ |
| Flyway runs as the owner role, the app as least-privilege | `FlywayOwnerCredentialsTest` (6) | ✅ |
| Secrets scan in CI | gitleaks job, green on main run 39780505513 | ✅ |
| Frontend: no tokens in `localStorage` / `sessionStorage` / Redux; no `NEXT_PUBLIC_*` secrets | `grep` over `frontend/src`: 0 hits | ✅ |

## Known gaps and accepted risks

| Item | Status |
|---|---|
| Login/register/logout UI journeys, axe, console guard | Pending (Playwright, other branch) |
| `LoginForm`, `RegisterForm`, `RequireAuth`, `UserMenu`, `proxy.ts` have no component tests | Pending E2E (see [frontend.md](frontend.md)) |
| ZAP baseline | Not run yet |
| Dependency-Check (NVD) runs weekly and on demand only (#18), not on every PR | Accepted (last green: run 39513438448) |

## Findings

| ID | Severity | Finding | Suggestion |
|---|---|---|---|
| F-AUTH-1 | Low | The `toString` mutants that return `""` survive on `PasswordHash`, `AccessToken`, `AuthenticateUserCommand` and `RegisterUserCommand`. The tests only check that the secret is **absent**, not what the redacted text is. Security is not weakened (an empty string leaks nothing). | Assert the exact redacted form, e.g. `PasswordHash[***]`. |
| F-AUTH-2 | Low | `ServerHardeningTest.should_ignore_a_spoofed_x_forwarded_for` only asserts `isNotEqualTo("203.0.113.30")`. It would also pass if the IP resolved to `null` or garbage. | Assert the real peer address (`127.0.0.1`). |
| F-AUTH-3 | Low | The nginx X-Forwarded-For overwrite (#35) is config-only and has no test. A regression to `$proxy_add_x_forwarded_for` would bring back IP spoofing of the rate limit. | A compose-profile smoke test (curl with a spoofed header → 4th register is still 429), or cover it in the ZAP/E2E stack. |
| F-AUTH-4 | Low | `SampleIT.context_loads` has no assertion (it is the P0 toolchain proof). It is now redundant because 16 other ITs boot the same context. | Delete it, or leave it with no gating role. |
| F-AUTH-5 | Medium | API security headers don't match security.md §4.7. Only `X-Content-Type-Options` and `X-Frame-Options` are asserted (`AuthorizationMatrixTest.should_send_the_security_headers`). A grep of `backend/*/src/main` finds no `headers()` customisation, so the API CSP (`default-src 'none'; frame-ancestors 'none'`), `Referrer-Policy` and `Permissions-Policy` look **unset** on API responses: Spring defaults only. `proxy.ts` sets them for pages only and skips `/api` ("Spring sets its own"). Not confirmed against a running stack. | Backend: configure the §4.7 headers. Tests: assert the full set (a ZAP baseline would also flag them). |
