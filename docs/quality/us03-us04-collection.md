# US03 / US04 — Collection (sync, list, get, update, delete): Verification Report

> Mode B · Test engineer · `origin/main` HEAD `0fa2732` (PR #37) · 2026-10-22
> Build totals are in [auth.md §Build](auth.md#build-numbers-shared-by-every-backend-report).

## Verdict

**Backend: PASS.** Every sync and update edge case in testing-strategy §5 has a passing test against real PostgreSQL (Testcontainers) and WireMock. **UI journeys: pending** (Playwright on another branch).

## Scope

| Item | Value |
|---|---|
| Endpoints | `POST /collection` (USER), `GET /collection?page&size&tag&q` (USER), `GET /collection/{pokedexId}` (USER), `PUT /collection/{pokedexId}` (USER), `DELETE /collection/{pokedexId}` (ADMIN) |
| Pages | `/collection`, `/collection/[pokedexId]`, `AddToCollectionButton`, `EditForm`, `DeleteDialog`, `RowActions` |
| PRs | #27 (collection API), #31 (transactions), #33 (re-sync refreshes catalog caches), #34 (collection UI), #36 (demo collection seed) |

## Acceptance criteria → tests

### US03 Sync

| Criterion (source) | Proven by | Result |
|---|---|---|
| First sync → 201 + `Location` + upstream snapshot; DTO lists exactly its fields | `SyncCollectionIT.should_create_collected_pokemon_with_201_location_and_upstream_snapshot` | ✅ |
| Re-sync → 200, one row, custom fields kept, snapshot + `syncedAt` refreshed, catalog cache refreshed (§5, BR-05) | `SyncCollectionIT.should_refresh_snapshot_with_200_and_keep_custom_attributes_when_resyncing`; `SyncPokemonServiceTest.should_preserve_custom_fields_when_resyncing`; `CatalogCacheRefresherTest` (4) | ✅ |
| Two syncs of the same id at once → one 201 + one 200, never a 500 (§5) | `SyncCollectionIT.should_keep_a_single_row_when_the_same_pokemon_is_synced_twice_at_once`; `SyncPokemonServiceTest` (3 concurrency tests); `JpaCollectedPokemonRepositoryAdapterTest.should_translate_a_concurrent_insert_into_a_version_conflict` | ✅ |
| Unknown to PokeAPI → 404, no row written (§5) | `SyncCollectionIT.should_return_404_and_write_nothing_when_pokemon_is_unknown_upstream` | ✅ |
| Upstream fails mid-sync → 503, no partial row (§5) | `SyncCollectionIT.should_return_503_and_write_nothing_when_pokeapi_is_down` (species call reset after the pokemon call succeeded) | ✅ |
| Invalid body (`{}`, 0, −1, `"abc"`, extra `name`, not JSON) → 400 | `SyncCollectionIT.should_reject_invalid_sync_body_with_400` | ✅ |
| Wrong `Content-Type` → 415 | `SyncCollectionIT.should_reject_sync_with_wrong_content_type_with_415` | ✅ |
| UI: signed-out "Add" → login → back (ui-ux §11) | Playwright | ⏳ pending |
| UI: re-sync keeps custom fields and a toast says so (ui-ux §11) | Playwright | ⏳ pending |

### Read (list / get)

| Criterion | Proven by | Result |
|---|---|---|
| Paging + exact tag filter + case-insensitive name search | `ReadCollectionIT.should_page_through_records_filtered_by_tag`, `…should_filter_by_exact_tag_only`, `…should_search_by_name_case_insensitively` | ✅ |
| `LIKE` wildcards in `q` (`%`, `_`, `\`) are literal (security §4.4) | `ReadCollectionIT.should_treat_like_wildcards_in_q_literally` (6); `LikePatternTest` (7) | ✅ |
| Invalid list params → 400; invalid path id → 400; missing → 404 | `ReadCollectionIT` (3 parameterized tests) | ✅ |
| Any USER reads records synced by another user (shared collection) | `ReadCollectionIT.should_let_any_user_read_records_synced_by_another_user` | ✅ |

### US04 Update

| Criterion (source) | Proven by | Result |
|---|---|---|
| Replace custom attributes, `version` + 1 | `UpdateCollectedPokemonIT.should_replace_custom_attributes_and_increment_version` | ✅ |
| Tags trimmed, lowercased, case-duplicates merged (§5) | `…should_normalize_tags_trimmed_lowercased_and_deduplicated`; `TagTest` (24) | ✅ |
| 11 tags, `"Fire Type!"`, 31-char tag, two names for one language, invalid language tag, 61-char value, blank value, lat 91, lat −90.0001, lon −181, lon 180.5, 61-char region, blank region → 400, nothing written (§5) | `…should_reject_invalid_custom_attributes_with_validation_problem` (13 cases, each also checks `version` is unchanged) | ✅ |
| 10 tags and boundary coordinates accepted | `…should_accept_ten_tags_and_boundary_coordinates` | ✅ |
| Change `name` / `stats` / `weight`, or an unknown field (top level or inside `customAttributes`) → 400 (BR-06) | `…should_reject_upstream_or_unknown_fields_with_400`, `…should_reject_unknown_field_inside_custom_attributes_with_400` | ✅ |
| Stale `version` → 409 (§5) | `…should_return_409_version_conflict_when_version_is_stale`; `UpdateCollectedPokemonServiceTest` | ✅ |
| Missing record → 404 | `…should_return_404_when_record_does_not_exist` | ✅ |
| Body > 16 KB → 413; non-JSON → 415 | `…should_reject_body_over_16_kb_with_413`, `…should_reject_non_json_content_type_with_415` | ✅ |
| SQL-injection / `<script>` text stored as plain text (§5, security §6) | `…should_store_injection_text_as_plain_text` | ✅ (backend; React escaping ⏳ E2E) |
| UI: latitude 91 → error summary gets focus, field shows the error (ui-ux §11) | `editForm.test.ts.should_reject_latitude_91` (schema only) | 🟡 schema ✅, focus ⏳ E2E |
| UI: stale version → conflict dialog → "Reload latest" (ui-ux §11) | Playwright | ⏳ pending |

### Delete

| Criterion | Proven by | Result |
|---|---|---|
| ADMIN → 204, then 404 | `DeleteCollectedPokemonIT.should_delete_as_admin_with_204_and_then_return_404` | ✅ |
| ADMIN on a missing record → 404 | `…should_return_404_when_admin_deletes_a_missing_record` | ✅ |
| USER → 403, record kept; 403 comes before the existence check (no oracle) | `…should_forbid_user_from_deleting_and_keep_the_record`; `DeleteCollectedPokemonServiceTest.should_forbid_a_user_before_revealing_whether_the_record_exists` | ✅ |
| UI: USER sees no Delete; ADMIN gets a confirmation and the row disappears (ui-ux §11) | Playwright | ⏳ pending |

## Coverage and mutation (collection slice)

| Package (JaCoCo aggregate, unit + IT) | Line | Branch |
|---|---|---|
| `domain.collection` | 65/65 = 100% | 16/16 = 100% |
| `domain.collection.vo` | 43/43 = 100% | 33/34 = 97.1% |
| `application.collection.command` / `.service` | 15/15, 47/47 = 100% | 10/10, 6/6 = 100% |
| `infrastructure.persistence` | 162/166 = 97.6% | 48/52 = 92.3% |
| `presentation.rest.collection` | 102/102 = 100% | 26/26 = 100% |

PIT: 3 surviving domain mutants in this slice, all "changed conditional boundary": `LocalizedName.<init>`, `LocalizedName.canonicalLanguage` and `GeoMetadata.optionalText` (see F-COL-1). Every application collection mutant was killed.

Tests in this slice: `UpdateCollectedPokemonIT` 29, `ReadCollectionIT` 28, `SyncCollectionIT` 14, `DeleteCollectedPokemonIT` 5; unit: `SyncPokemonServiceTest` 11, `CollectionQueryTest` 12, `CollectionControllerTest` 17, `CollectionDtoMapperTest` 15, `JpaCollectedPokemonRepositoryAdapterTest` 11, `CollectedPokemonEntityMapperTest` 10, `LocalizedNameTest` 31, `TagTest` 24, `CoordinatesTest` 14, `GeoMetadataTest` 13; frontend `editForm.test.ts` 18.

## Security checks

| Check | Evidence | Result |
|---|---|---|
| Matrix rows: anonymous → 401 (reads + writes), USER → 200/201/404 and DELETE 403, ADMIN → every endpoint | `AuthorizationMatrixIT` (collection section), `CollectionAuthorizationMatrixTest` (19), `ReadCollectionIT.should_reject_anonymous_reads_with_401` | ✅ |
| CSRF on POST/PUT/DELETE, even when authenticated | `CollectionCsrfTest` (3), `SyncCollectionIT.should_reject_sync_without_csrf_token`, `UpdateCollectedPokemonIT.should_reject_update_without_csrf_token`, `DeleteCollectedPokemonIT.should_reject_admin_delete_without_csrf_token` | ✅ |
| 16 KB body limit → 413 | `UpdateCollectedPokemonIT.should_reject_body_over_16_kb_with_413`; `RequestBodyLimitFilterTest` (6) | ✅ |
| nginx edge allows 32 KB (`client_max_body_size 32k`), so the app's 16 KB limit and its ProblemDetail answer it first | `docker/nginx/default.conf` (review) | ✅ |
| Mass assignment / unknown fields → 400 | see US04 table | ✅ |
| No string-built queries; `LIKE` escaped | `CollectionSpecificationsTest`, `LikePatternTest`, `ReadCollectionIT` wildcard test | ✅ |
| Use cases are transactional (rollback on failure) | `TransactionalUseCaseTest` (4) | ✅ |

## Known gaps and accepted risks

| Item | Status |
|---|---|
| All collection UI journeys (§11 US03/US04/Auth rows), axe, console guard | Pending (Playwright) |
| `EditForm`, `ErrorSummary`, `DeleteDialog`, `CollectionList`, `RowActions`, `AddToCollectionButton` have no component tests | Pending E2E (see [frontend.md](frontend.md)) |
| ZAP baseline | Not run yet |

## Findings

| ID | Severity | Finding | Suggestion |
|---|---|---|---|
| F-COL-1 | Low | 3 boundary mutants survive. `LocalizedName` (`length < 1 \|\| length > MAX_VALUE_LENGTH` and `candidate.length() > MAX_LANG_LENGTH`) and `GeoMetadata.optionalText` have no unit test at the exact boundary (exactly 1 or exactly max code points). The IT covers 61 chars → 400, but nothing proves 60 → OK at domain level. | Add exact-boundary cases to `LocalizedNameTest` / `GeoMetadataTest`. |
| F-COL-2 | Low | `CollectionSpecificationsTest` (4 tests) uses only Mockito `verify(cb).and(...)` / `never()` on a mocked `CriteriaBuilder`, with no state assertion. It pins the implementation, not the behaviour. Real behaviour **is** covered by `ReadCollectionIT` against PostgreSQL, so there is no false-green risk. | Accept, or replace with a `@DataJpaTest` + Testcontainers check. |
| F-COL-3 | Info | `SyncCollectionIT.should_keep_a_single_row_when_the_same_pokemon_is_synced_twice_at_once` relies on a 300 ms WireMock delay to overlap the requests. If they serialise, it still passes (201 + 200), so it can't flake red, but it may not always exercise the race. The race path itself is proven deterministically in `SyncPokemonServiceTest` and `JpaCollectedPokemonRepositoryAdapterTest`. | None needed. |
