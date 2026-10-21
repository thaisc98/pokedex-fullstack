# Recorded PokeAPI fixtures

Owner: test-engineer. Real responses from `https://pokeapi.co/api/v2`, recorded with `curl` on
2026-10-20 (US01; PokeAPI `count` = 1351 at that time) and 2026-10-21 (US02). **No file here has been hand-edited.** The bodies
are byte-for-byte what PokeAPI returned, so they still contain absolute `https://pokeapi.co/...` URLs.
The app must never follow those URLs (security.md §4.5); the acceptance tests check that every call
lands on the configured base URL (WireMock).

Re-record with:

```bash
B=https://pokeapi.co/api/v2
curl -sf "$B/pokemon?offset=<o>&limit=<l>" -o pokemon-list/offset-<o>-limit-<l>.json
curl -sf "$B/pokemon/<id>"                 -o pokemon/<id>.json
curl -sf "$B/pokemon-species/<id>"         -o pokemon-species/<id>.json
curl -sf "$B/evolution-chain/<id>"         -o evolution-chain/<id>.json
curl -s  "$B/pokemon/99999"                -o errors/not-found.json   # real 404 body
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
| `pokemon-species/1007.json` | `/pokemon-species/1007` | Koraidon, genus "Paradox Pokémon"; chain 535 |
| `pokemon/25.json`, `pokemon-species/25.json` | `/pokemon/25`, `/pokemon-species/25` | US02 happy path (pikachu): 4 dm, 60 hg, electric; latest English text is `legends-arceus`; old `red` entry has `\f` and `\n` (BR-03) |
| `pokemon/133.json`, `pokemon-species/133.json` | `/pokemon/133`, `/pokemon-species/133` | Eevee (8-way branch), chain 67 |
| `pokemon/122.json`, `pokemon-species/122.json` | `/pokemon/122`, `/pokemon-species/122` | Mr. Mime: hyphenated name `mr-mime`, two types, chain 57 |
| `evolution-chain/10.json` | `/evolution-chain/10` | Pichu (baby) → Pikachu (level-up, friendship) → Raichu (thunder-stone) |
| `evolution-chain/67.json` | `/evolution-chain/67` | Eevee → 8 branches. Leafeon/Glaceon list several `evolution_details` (level-up near a rock **and** use-item with `is_default`) |
| `evolution-chain/57.json` | `/evolution-chain/57` | Mime Jr. (baby) → Mr. Mime (level-up, knows Mimic) → Mr. Rime (level-up, `min_level` 42) |
| `evolution-chain/1.json` | `/evolution-chain/1` | Bulbasaur line, for the per-resource cache test (list then detail) |
| `evolution-chain/535.json` | `/evolution-chain/535` | Koraidon: single stage, no evolutions |
| `errors/not-found.json` | `/pokemon/99999` | PokeAPI's real 404 body `{"status":404,"message":"Not Found"}` (served with status 404) |

## Re-mapped (not edited) fixtures

A real body served for a **different** query than the one it was recorded from. The JSON is unchanged.

| Test | Query stubbed | Body served | Why |
|---|---|---|---|
| `ListPokemonIT.should_default_to_page_0_and_size_20_when_paging_params_are_absent` | `offset=0&limit=20` | `offset-2000000-limit-20.json` | Proves only which defaults reach PokeAPI and the response echoes `page=0,size=20`. A real first page would need 40 more ~250 KB files. |
| `ListPokemonIT.should_return_empty_page_without_overflowing_offset_when_page_is_huge` | any non-negative `offset`, `limit=100` | `offset-2000000-limit-20.json` | Any real offset that large returns the same empty body. |
| `ListPokemonIT.should_accept_size_100_as_the_inclusive_upper_bound` | `offset=10000000&limit=100` | `offset-2000000-limit-20.json` | Same: past the end, empty results. |

## Variants built inside tests (nothing committed)

PokeAPI has no real example for these, so the test loads a real fixture, changes one thing with Jackson
and names the change in the test:

| Test | Change |
|---|---|
| `GetPokemonDetailIT.should_fall_back_to_sprite_when_official_artwork_is_missing` | `pokemon/25.json` with `sprites.other.official-artwork.front_default` = `null`. A scan of ids 1000–1025 and 10001–10326 on 2026-10-21 found no real Pokémon that has a sprite but no artwork |
| `GetPokemonDetailIT.should_clean_form_feeds_and_newlines_in_description` | `pokemon-species/25.json` keeping only the English `red` flavor text |
| `GetPokemonDetailIT.should_return_empty_description_when_species_has_no_english_flavor_text` | `pokemon-species/25.json` with every English flavor text removed |
| `PokeApiResponseSizeLimitIT`, `GetPokemonDetailUpstreamFailureIT` (`OVER_1_MB`) | Real fixture padded with an unknown `_padding` field to 1.2 MB / 900 KB |

`koraidon-limited-build` (`pokemon/10264.json`) is the **real** case with neither artwork nor sprite (placeholder).

## Sizes

`/pokemon/{id}` bodies are ~250–310 KB each (mostly `moves`). Keep additions to what a test needs.
