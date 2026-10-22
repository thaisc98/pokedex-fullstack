# US01 — List Pokémon: Verification Report

> Mode B (verify after implementation) · Test engineer
> Verified against `origin/main` HEAD `0fa2732` (PR #37) · Date: 2026-10-22
> Numbers come from `./mvnw -B verify` and `./mvnw -B -pl domain,application -Pmutation verify`, run in this worktree. Shared backend totals are in [auth.md §Build](auth.md#build-numbers-shared-by-every-backend-report).

## Verdict

**Backend: PASS.** Every catalog-list acceptance criterion and §5 edge case has a passing test. **UI journeys (ui-ux.md §11): pending** until Playwright E2E lands; it is being built on another branch.

## Scope

| Item | Value |
|---|---|
| Endpoint | `GET /api/v1/pokemon?page&size` (public) |
| Page | `/pokemon` (Server Component, `CatalogPagination`, `PokemonCard`) |
| PRs | #17 (US01 list), #24 (1 MB PokeAPI limit, springdoc contract check), #29 (catalog UI), #33 (re-sync refreshes caches), #37 (offline WireMock PokeAPI) |

## Acceptance criteria → tests

| Criterion (source) | Proven by | Result |
|---|---|---|
| Page of summaries: sprite, name, category, mass, abilities (UC-01) | `ListPokemonIT.should_return_page_of_summaries_built_from_pokeapi_when_page_and_size_are_valid` | ✅ |
| Response lists only the contract fields | `ListPokemonIT.should_expose_only_the_contract_fields_on_each_summary` | ✅ |
| Defaults `page=0,size=20` | `ListPokemonIT.should_default_to_page_0_and_size_20_when_paging_params_are_absent` | ✅ |
| `page=-1`, `size=0`, `size=101`, `page=abc` → 400 ProblemDetail (§5) | `ListPokemonIT.should_reject_invalid_paging_with_validation_problem` (parameterized) | ✅ |
| `size=100` accepted (inclusive bound) | `ListPokemonIT.should_accept_size_100_as_the_inclusive_upper_bound` | ✅ |
| Page past the end → 200 with empty `content` (§5) | `ListPokemonIT.should_return_empty_page_when_page_is_past_the_end`, `…_without_overflowing_offset_when_page_is_huge` | ✅ |
| Null `front_default` → placeholder (§5) | `ListPokemonIT.should_use_placeholder_sprite_when_front_default_is_null` | ✅ |
| Mass 0 hg / 9999 hg rounds the same way (§5) | `MassTest.should_convert_hectograms_to_kilograms_rounded_to_one_decimal`, `should_never_produce_more_than_one_decimal_place` | ✅ |
| Genus with no English entry → `Category.UNKNOWN` (§5) | `PokeApiMapperTest` (2 cases asserting `isSameAs(Category.UNKNOWN)`) | ✅ |
| Upstream 5xx / 429 → 503 (§5) | `ListPokemonUpstreamFailureIT.should_return_503_problem_when_pokeapi_list_endpoint_fails` (500, 502, 503, 504, 429) | ✅ |
| Connection reset / empty / garbage → 503 | `ListPokemonUpstreamFailureIT.should_return_503_problem_when_pokeapi_is_unreachable` | ✅ |
| Malformed JSON → 503, no stack trace | `ListPokemonUpstreamFailureIT.should_return_503_problem_without_stack_trace_when_pokeapi_returns_malformed_json` | ✅ |
| One failing per-Pokémon / species call → 503, never a partial page | `…_instead_of_partial_page_when_one_pokemon_call_fails`, `…_when_one_species_call_fails` | ✅ |
| Retried on 5xx/IO, never on 4xx | `ListPokemonUpstreamFailureIT.should_retry_pokeapi_on_5xx_before_giving_up`; `PokeApiGatewayResilienceTest` (10 tests: 3 retries on 5xx, IO retry, no retry on 404/429/oversized/unreadable) | ✅ (see F-US01-1) |
| Second identical call → **zero** upstream calls (§5, ADR 0004) | `ListPokemonIT.should_make_zero_upstream_calls_when_the_same_page_is_requested_twice`; `CachingPokemonCatalogTest` (6) | ✅ |
| Failures are not cached | `CachingPokemonCatalogTest.should_rethrow_the_original_failure_and_not_cache_it` | ✅ |
| PokeAPI URLs are rebuilt against the configured base, never followed (SSRF) | `ListPokemonIT.should_rebuild_upstream_urls_against_configured_base_instead_of_following_them` | ✅ |
| PokeAPI response > 1 MB → 503; just under 1 MB accepted | `PokeApiResponseSizeLimitIT` (3), `ResponseSizeLimitInterceptorTest` (12), `PokeApiResponseLimitTest` (3) | ✅ |
| Anonymous access, no `JSESSIONID` | `ListPokemonIT.should_allow_anonymous_access_without_cookie_or_credentials`; `AuthorizationMatrixTest.should_never_set_a_session_cookie_for_the_public_catalog` | ✅ |
| Implemented operation matches `openapi.yaml` | `OpenApiContractIT` (4) | ✅ |
| UI: 375 px → 2 columns; keyboard pagination (ui-ux §11) | Playwright | ⏳ pending |
| UI: Back from page 2 restores page 1 + scroll (ui-ux §11) | Playwright; URL building covered by `catalogParams.test.ts` (15) | ⏳ pending (unit part ✅) |
| UI: no console warnings, 0 serious/critical axe | Playwright console guard + axe | ⏳ pending |

## Coverage and mutation (catalog slice)

| Package (JaCoCo aggregate, unit + IT) | Line | Branch |
|---|---|---|
| `domain.catalog` | 46/46 = 100% | 18/18 = 100% |
| `domain.catalog.vo` | 150/150 = 100% | 79/82 = 96.3% |
| `application.catalog.service` | 10/10 = 100% | n/a |
| `infrastructure.pokeapi` | 255/259 = 98.5% | 79/90 = 87.8% |
| `infrastructure.cache` | 39/39 = 100% | 9/10 = 90.0% |
| `presentation.rest.catalog` | 54/54 = 100% | 10/10 = 100% |

PIT: every catalog mutant was killed. The 5 surviving domain mutants are all in `identity` and `collection` (see [auth.md](auth.md) and [us03-us04-collection.md](us03-us04-collection.md)).

Tests in this slice: `ListPokemonIT` 15, `ListPokemonUpstreamFailureIT` 12, `PokeApiResponseSizeLimitIT` 3, `ListPokemonServiceTest` 7, `CatalogControllerTest` 14, `CatalogDtoMapperTest` 4, `PokeApiCatalogAdapterTest` 17, `PokeApiMapperTest` 28.

## Security checks

| Check | Evidence | Result |
|---|---|---|
| Matrix row: anonymous `GET /pokemon` → 200; writes → 401/403 | `AuthorizationMatrixTest` (anonymous rows), `AuthorizationMatrixIT.should_have_a_matrix_row_for_every_served_endpoint` | ✅ anonymous. USER/ADMIN rows missing, see F-US01-2 |
| CSRF on a non-existent write (`POST /pokemon`) → 403 `csrf-token-invalid` | `AuthorizationMatrixIT.should_reject_write_without_csrf_token_with_csrf_problem` | ✅ |
| 1 MB upstream cap | `PokeApiResponseSizeLimitIT` | ✅ |
| SSRF: upstream URLs never followed | see above | ✅ |
| Security headers (`nosniff`, `X-Frame-Options: DENY`) | `AuthorizationMatrixTest.should_send_the_security_headers` | ✅ |

## Known gaps and accepted risks

| Item | Status |
|---|---|
| Playwright journeys at 375/768/1280, axe, console guard | Pending (other branch) |
| Lighthouse CI on the catalog | Not configured yet |
| Sprites in the offline profile point at `raw.githubusercontent.com`, so they don't load without network | Accepted (offline demo, #37); the placeholder fallback covers it |
| ZAP baseline | Not run yet (P6) |

## Findings

| ID | Severity | Finding | Suggestion |
|---|---|---|---|
| F-US01-1 | Low | `testing-strategy.md` §5 says upstream **429 is retried**. The code and `PokeApiGatewayResilienceTest.should_not_retry_on_429` say it is **not** retried (503 straight away). The test is consistent with the code, but not with the doc. | Human decides: update §5 (not hammering a rate-limited upstream is the safer choice) or change the behaviour. |
| F-US01-2 | Medium | The authorization matrix has only the **anonymous** row for `GET /pokemon` and `GET /pokemon/{idOrName}`. No test calls them as USER or ADMIN (the rule is endpoint × {anonymous, USER, ADMIN}). If a cookie were present, a bad JWT filter order could break public reads for signed-in users, and no test would catch it. | Add USER and ADMIN rows → 200 in `AuthorizationMatrixIT`. |
| F-US01-3 | Low | The production cache cap (`PokeApiConfig` `maximumSize`) is not asserted. Cache tests build their own Caffeine instance, and `bootstrap` line coverage is 52.2%. | A bootstrap test that reads the configured cache's policy. |
