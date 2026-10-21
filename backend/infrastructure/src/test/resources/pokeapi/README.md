# Recorded PokeAPI fixtures

Owner: test-engineer. Real responses from `https://pokeapi.co/api/v2`, recorded with `curl` on
2026-10-20 (PokeAPI `count` = 1351 at that time). **No file here has been hand-edited.** The bodies
are byte-for-byte what PokeAPI returned, so they still contain absolute `https://pokeapi.co/...` URLs.
The app must never follow those URLs (security.md §4.5); the acceptance tests check that every call
lands on the configured base URL (WireMock).

Re-record with:

```bash
B=https://pokeapi.co/api/v2
curl -sf "$B/pokemon?offset=<o>&limit=<l>" -o pokemon-list/offset-<o>-limit-<l>.json
curl -sf "$B/pokemon/<id>"                 -o pokemon/<id>.json
curl -sf "$B/pokemon-species/<id>"         -o pokemon-species/<id>.json
```

## Files

| File | Source URL | Why |
|---|---|---|
| `pokemon-list/offset-0-limit-3.json` | `/pokemon?offset=0&limit=3` | US01 happy path: bulbasaur, ivysaur, venusaur |
| `pokemon-list/offset-1-limit-1.json` | `/pokemon?offset=1&limit=1` | Cache test (ivysaur), own page key |
| `pokemon-list/offset-3-limit-1.json` | `/pokemon?offset=3&limit=1` | Partial-page failure: charmander's `/pokemon` call fails |
| `pokemon-list/offset-4-limit-1.json` | `/pokemon?offset=4&limit=1` | Partial-page failure: charmeleon's species call fails |
| `pokemon-list/offset-1288-limit-1.json` | `/pokemon?offset=1288&limit=1` | Page holding koraidon-limited-build (null sprite) |
| `pokemon-list/offset-2000000-limit-20.json` | `/pokemon?offset=2000000&limit=20` | Page past the end: `count` 1351, `results: []` (BR-01) |
| `pokemon/1.json` … `pokemon/3.json` | `/pokemon/{id}` | Bulbasaur 69 hg → 6.9 kg (BR-02), ivysaur 130 hg, venusaur 1000 hg; one hidden ability each |
| `pokemon/5.json` | `/pokemon/5` | Charmeleon (other half of the partial-page case) |
| `pokemon/10264.json` | `/pokemon/10264` | **Real** null `sprites.front_default` (koraidon-limited-build); species id 1007 ≠ Pokémon id; 3030 hg |
| `pokemon-species/1.json` … `pokemon-species/4.json` | `/pokemon-species/{id}` | Genus "Seed Pokémon" / "Lizard Pokémon" |
| `pokemon-species/1007.json` | `/pokemon-species/1007` | Koraidon, genus "Paradox Pokémon" |

## Re-mapped (not edited) fixtures

A real body served for a **different** query than the one it was recorded from. The JSON is unchanged.

| Test | Query stubbed | Body served | Why |
|---|---|---|---|
| `ListPokemonIT.should_default_to_page_0_and_size_20_when_paging_params_are_absent` | `offset=0&limit=20` | `offset-2000000-limit-20.json` | Proves only which defaults reach PokeAPI and the response echoes `page=0,size=20`. A real first page would need 40 more ~250 KB files. |
| `ListPokemonIT.should_return_empty_page_without_overflowing_offset_when_page_is_huge` | any non-negative `offset`, `limit=100` | `offset-2000000-limit-20.json` | Any real offset that large returns the same empty body. |
| `ListPokemonIT.should_accept_size_100_as_the_inclusive_upper_bound` | `offset=10000000&limit=100` | `offset-2000000-limit-20.json` | Same: past the end, empty results. |

## Sizes

`/pokemon/{id}` bodies are ~250–310 KB each (mostly `moves`). Keep additions to what a test needs.
