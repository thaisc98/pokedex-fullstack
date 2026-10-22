# US02 — Pokémon Detail: Verification Report

> Mode B · Test engineer · `origin/main` HEAD `0fa2732` (PR #37) · 2026-10-22
> Build totals are in [auth.md §Build](auth.md#build-numbers-shared-by-every-backend-report).

## Verdict

**Backend: PASS.** **UI journeys: pending** (Playwright on another branch). One accepted UX/HTTP risk: the not-found page is served with HTTP 200.

## Scope

| Item | Value |
|---|---|
| Endpoint | `GET /api/v1/pokemon/{idOrName}` (public) |
| Page | `/pokemon/[idOrName]` (+ `loading.tsx`, `not-found.tsx`, catalog `error.tsx`) |
| PRs | #25 (US02 detail), #32 (abilities on the detail), #29 (detail page), #33, #37 |

## Acceptance criteria → tests

| Criterion (source) | Proven by | Result |
|---|---|---|
| Full detail: image, stats, description, evolution chain, abilities (UC-02) | `GetPokemonDetailIT.should_return_full_detail_for_pikachu_by_id` | ✅ |
| Same detail by id, name and mixed case (`Pikachu` → lowercase) (§5) | `GetPokemonDetailIT.should_return_the_same_detail_for_id_name_and_mixed_case_name` | ✅ |
| `mr-mime` valid (§5) | `GetPokemonDetailIT.should_accept_hyphenated_name_mr_mime` | ✅ |
| `0`, `-5`, `99999` → 404 `/problems/not-found` (§5) | `GetPokemonDetailIT.should_return_not_found_problem_when_pokemon_does_not_exist` (parameterized) | ✅ |
| `pika;drop` → 400; other malformed names → 400 | `…should_reject_name_with_semicolon_with_bad_request_problem`, `…should_reject_malformed_name_with_validation_problem` | ✅ |
| Missing official artwork → sprite; both missing → placeholder (§5) | `…should_fall_back_to_sprite_when_official_artwork_is_missing`, `…should_use_placeholder_artwork_when_artwork_and_sprite_are_both_missing` | ✅ |
| Flavor text with `\f`/`\n` cleaned; no English entry → empty (§5) | `…should_clean_form_feeds_and_newlines_in_description`, `…should_return_empty_description_when_species_has_no_english_flavor_text`; `DescriptionTest` (16) | ✅ |
| Eevee: all 8 branches, in upstream order (ui-ux §11) | `GetPokemonDetailIT.should_keep_all_eight_eevee_branches_in_upstream_order`; UI: `EvolutionTree.test.tsx.should_render_all_eight_eevee_branches_as_links` | ✅ (API + component) |
| Chains: none, linear 3-stage, branched, baby, forms (§5) | `EvolutionChainTest` (12), `PokeApiDetailMapperTest` (21), `EvolutionTree.test.tsx` (linear, no-evolution, triggers) | ✅ |
| Species/chain URLs rebuilt from ids, never followed (SSRF) | `…should_rebuild_species_and_chain_urls_from_ids_instead_of_following_them` | ✅ |
| Any one upstream call failing → 503, no partial detail | `GetPokemonDetailUpstreamFailureIT` (12) | ✅ |
| Second call → zero upstream calls; reuses resources cached by the list | `…should_make_zero_upstream_calls_when_the_same_detail_is_requested_twice`, `…should_reuse_pokemon_and_species_cached_by_the_list_when_detail_is_requested`; `CachingPokeApiResourcesTest` (6) | ✅ |
| Not-found is not cached | `CachingPokeApiResourcesTest.should_rethrow_a_failure_unchanged_and_not_cache_it` | ✅ |
| Retries name lookup + chain on 5xx, never on 404 | `PokeApiGatewayResilienceTest.should_retry_the_name_lookup_and_evolution_chain_calls_but_not_a_404` | ✅ |
| Anonymous access | `GetPokemonDetailIT.should_allow_anonymous_access_to_detail` | ✅ |
| UI: `/pokemon/does-not-exist` → friendly not-found + link to catalog (ui-ux §11) | Playwright | ⏳ pending (see accepted risk) |
| UI: each evolution stage is a link; current stage has `aria-current` | `EvolutionTree.test.tsx` (10) | ✅ component / ⏳ E2E |
| UI: loading / error states (ui-ux §7) | `loading.tsx`, `error.tsx` exist; no test | ⏳ pending |

## Coverage and mutation

Shared catalog packages: see [us01-list.md](us01-list.md#coverage-and-mutation-catalog-slice). Detail-specific classes are fully covered: `PokemonDetail`, `EvolutionChain` and `PokeApiDetailMapper` are inside `domain.catalog` (100% line / 100% branch) and `infrastructure.pokeapi` (98.5% / 87.8%). PIT killed every catalog mutant.

Tests in this slice: `GetPokemonDetailIT` 21, `GetPokemonDetailUpstreamFailureIT` 12, `GetPokemonDetailServiceTest` 7, `CatalogDetailControllerTest` 13, `CatalogDetailDtoMapperTest` 5, `PokeApiCatalogAdapterDetailTest` 8, `PokeApiDetailMapperTest` 21, `EvolutionTree.test.tsx` 10, `TypeBadge.test.tsx` 2.

## Security checks

| Check | Evidence | Result |
|---|---|---|
| Matrix: anonymous `GET /pokemon/{idOrName}` → 200 | `AuthorizationMatrixTest`, `GetPokemonDetailIT` | ✅ (USER/ADMIN rows missing, same as F-US01-2) |
| Input validation before any upstream call (`pika;drop`) | see above | ✅ |
| SSRF: rebuilt URLs | see above | ✅ |
| Image URLs pass `isSafeHttpUrl` / `isSpriteUrl` in the UI | `isSafeHttpUrl.test.ts` (17) | ✅ |
| 1 MB upstream cap applies to the detail calls too | `ResponseSizeLimitInterceptorTest` (interceptor is shared) | ✅ |

## Known gaps and accepted risks

| Item | Status |
|---|---|
| **The not-found page returns HTTP 200.** `notFound()` runs after `loading.tsx` has started streaming, so the status is already sent. The user still sees the friendly page. | Accepted risk (crawlers / monitoring see 200) |
| Sprites/artwork don't load in the offline profile (GitHub-hosted) | Accepted; the `Sprite` placeholder covers it |
| Playwright, axe, Lighthouse on the detail page | Pending |
| ZAP baseline | Not run yet |

## Findings

| ID | Severity | Finding | Suggestion |
|---|---|---|---|
| F-US02-1 | Low | The detail Server Component has no test (0% in an all-source Vitest run). That includes its 400/404 → `notFound()` mapping and the 503 → `ErrorState` branch. | Cover it in the Playwright journey (`/pokemon/does-not-exist`, a PokeAPI-down stub). |
| F-US02-2 | Low | Same as F-US01-2: no USER/ADMIN matrix row for the public detail. | Add the rows. |
