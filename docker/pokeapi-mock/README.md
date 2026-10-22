# Offline PokeAPI (WireMock, Generation 1)

Owner: test-engineer. Served by the `pokeapi-mock` service in [docker-compose.offline.yml](../../docker-compose.offline.yml)
(how to run it: root README, "Offline demo (no internet)"). The backend reaches it at
`POKEAPI_BASE_URL=http://pokeapi-mock:8080/api/v2`.

**Everything in `mappings/` and `__files/` is generated** by [record-offline-pokeapi.sh](record-offline-pokeapi.sh)
from real PokeAPI responses. Never edit those files by hand: change the script and re-run it.

```bash
docker/pokeapi-mock/record-offline-pokeapi.sh      # needs internet, bash, curl, jq; rebuilds both folders
```

Last recorded: 2026-10-22 from `https://pokeapi.co/api/v2`.

## What is served

| Request | Answer |
|---|---|
| `GET /api/v2/pokemon/{id or name}` | Recorded body, for ids 1–151 and every species in their evolution chains (188 Pokémon: 1–151 plus 169, 172–174, 182, 186, 196, 197, 199, 208, 212, 230, 233, 236–240, 242, 439, 440, 446, 462–467, 470, 471, 474, 700, 863, 865, 866, 900, 979). Names match case-insensitively and a trailing slash is allowed, like PokeAPI. |
| `GET /api/v2/pokemon-species/{id or name}` | Same 188 species. |
| `GET /api/v2/evolution-chain/{id}` | The 78 chains those species belong to (e.g. 67 = Eevee and its 8 evolutions). |
| `GET /api/v2/pokemon?offset=O&limit=L` | `count: 151` and `results` = entries `O … O+L-1` of the recorded Gen 1 list (`name` + `url`). Built by response templating (`__files/pokemon-list/page.json.hbs`: a JSONPath `[O:O+L]` slice), so every page size the UI allows (1–100), every page, and the name search's single `offset=0&limit=100000` call (the whole Gen 1 list) work. An offset past 151 gives `results: []`; offsets of 10+ digits hit a static empty page. |
| Anything else (#152+, unknown names, other resources) | `404` with PokeAPI's real body `{"status":404,"message":"Not Found"}`. |

The response `url`s still point at `https://pokeapi.co/...`, exactly as recorded. The backend only extracts ids from
them and rebuilds requests against its configured base URL (security.md §4.5), so nothing is ever fetched from there.

## Transformations (applied by the script with jq)

To keep the repo small (about 2.1 MB in ~460 files) the bodies keep only what the backend reads
(`backend/infrastructure/.../pokeapi/dto`). Values are never changed except where noted:

| Resource | Kept | Dropped |
|---|---|---|
| `/pokemon/{id}` | `id, name, height, weight, abilities, stats, types, species`, `sprites.front_default`, `sprites.other.official-artwork.front_default` | `moves`, `game_indices`, `held_items`, `past_*`, `cries`, `forms`, every other sprite (~98% of the body) |
| `/pokemon-species/{id}` | `id, name, evolution_chain`, **English** `genera` and `flavor_text_entries` | every other field; non-English genera/flavor texts (the backend filters on `en`) |
| `/evolution-chain/{id}` | `id, baby_trigger_item, chain` (complete) | nothing else exists |
| `/pokemon?offset=0&limit=151` | `results` | `next`, `previous` (never read). **`count` is set to 151** (real value: 1351) so the offline catalog ends at Mew. |
| `/pokemon/99999` | the whole 404 body | none |

Sizes: a stripped `/pokemon/{id}` is ~4 KB (real: ~280 KB), a species ~10 KB.

## Limits (by design)

- Only Generation 1 and its evolution relatives. Anything else is a 404.
- Sprite and artwork URLs point at `raw.githubusercontent.com`; the browser loads them, so offline the UI shows its
  image fallback.
- These files are for the offline demo. The backend ITs keep their own unstripped fixtures in
  `backend/infrastructure/src/test/resources/pokeapi/`.
