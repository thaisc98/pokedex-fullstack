#!/usr/bin/env bash
# Records the offline PokeAPI (Generation 1) served by the `pokeapi-mock` WireMock service
# (docker-compose.offline.yml). Owner: test-engineer. Documented in README.md next to this script.
#
#   docker/pokeapi-mock/record-offline-pokeapi.sh        # needs internet, bash, curl and jq
#
# Idempotent: it deletes and rebuilds __files/ and mappings/ from scratch. Nothing in them is edited by hand.
#   1. GET /pokemon/{id} and /pokemon-species/{id} for ids 1..LAST_ID (151 = Generation 1).
#   2. GET /evolution-chain/{id} for every chain those species belong to, then /pokemon and /pokemon-species
#      for every species in those chains (Pichu 172, Crobat 169, Sylveon 700...), so every evolution link on
#      a detail page resolves offline.
#   3. GET /pokemon?offset=0&limit=LAST_ID: the catalog list, sliced per request by a templated mapping.
#   4. GET /pokemon/99999: PokeAPI's real 404 body, served for anything that was not recorded.
# Bodies are stripped with jq to the fields the backend reads (infrastructure.pokeapi.dto); see README.md
# "Transformations" for the full list.
set -euo pipefail

BASE="${POKEAPI_RECORD_BASE:-https://pokeapi.co/api/v2}"
LAST_ID="${LAST_ID:-151}"
HERE="$(cd "$(dirname "$0")" && pwd)"
FILES="$HERE/__files"
MAPPINGS="$HERE/mappings"

command -v jq >/dev/null || { echo "jq is required" >&2; exit 1; }
command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }

rm -rf "$FILES" "$MAPPINGS"
mkdir -p "$FILES/pokemon" "$FILES/pokemon-species" "$FILES/evolution-chain" "$FILES/pokemon-list" \
  "$FILES/errors" "$MAPPINGS"

fetch() { curl -sSf --retry 3 --retry-delay 2 -H 'Accept: application/json' "$BASE$1"; }

# Fields read by PokeApiPokemonDto. Sprites keep only front_default and the official artwork.
POKEMON_JQ='{id, name, height, weight,
  sprites: {front_default: .sprites.front_default,
            other: {"official-artwork": {front_default: .sprites.other."official-artwork".front_default}}},
  abilities, stats, types, species}'
# Fields read by PokeApiSpeciesDto. The backend only uses English genera and flavor texts (PokeApiMapper
# filters on language "en"), so the other languages are dropped: they are most of each body.
SPECIES_JQ='{id, name,
  genera: [.genera[] | select(.language.name == "en")],
  flavor_text_entries: [.flavor_text_entries[] | select(.language.name == "en")],
  evolution_chain}'

record_species() { # $1 = species id; its default Pokémon has the same id
  local id="$1"
  [[ -f "$FILES/pokemon-species/$id.json" ]] && return 0
  fetch "/pokemon/$id" | jq -c "$POKEMON_JQ" > "$FILES/pokemon/$id.json"
  fetch "/pokemon-species/$id" | jq -c "$SPECIES_JQ" > "$FILES/pokemon-species/$id.json"
}

echo "Recording /pokemon and /pokemon-species 1..$LAST_ID"
for id in $(seq 1 "$LAST_ID"); do record_species "$id"; done

echo "Recording the evolution chains and every species they reference"
CHAINS="$(jq -r '.evolution_chain.url | capture("/evolution-chain/(?<id>[0-9]+)/?$").id' \
  "$FILES"/pokemon-species/*.json | sort -un)"
for chain in $CHAINS; do
  fetch "/evolution-chain/$chain" | jq -c '{id, baby_trigger_item, chain}' > "$FILES/evolution-chain/$chain.json"
  for sid in $(jq -r '.. | objects | select(has("species")) | .species.url
                      | capture("/pokemon-species/(?<id>[0-9]+)/?$").id' "$FILES/evolution-chain/$chain.json"); do
    record_species "$sid"
  done
done

echo "Recording the list and the 404 body"
# count becomes LAST_ID: the offline catalog ends at Mew. next/previous are dropped (the backend never reads them).
LIST="$(fetch "/pokemon?offset=0&limit=$LAST_ID" | jq -c --argjson n "$LAST_ID" '{count: $n, results}')"
[[ "$(jq '.results | length' <<<"$LIST")" == "$LAST_ID" ]] || { echo "list is not $LAST_ID long" >&2; exit 1; }
curl -sS "$BASE/pokemon/99999" > "$FILES/errors/not-found.json"
jq -e '.status == 404' "$FILES/errors/not-found.json" >/dev/null

# The list template: WireMock response templating slices `results` with a JSONPath [offset:offset+limit] slice
# (an offset past the end gives []). No space between the last two braces would break the Handlebars parser.
{
  printf "{{#parseJson 'all'}}%s{{/parseJson}}" "$LIST"
  printf "{{#assign 'end'}}{{math request.query.offset '+' request.query.limit}}{{/assign}}"
  printf "{{#assign 'path'}}\$.results[{{request.query.offset}}:{{end}}]{{/assign}}"
  printf '{"count":{{all.count}},"next":null,"previous":null,"results":{{toJson (jsonPath all path)}} }\n'
} > "$FILES/pokemon-list/page.json.hbs"

echo "Generating mappings"
HEADERS='{"Content-Type": "application/json; charset=utf-8"}'

# /pokemon/{id} and /pokemon/{name}. Like PokeAPI: names match case-insensitively, a trailing slash is allowed.
resource_mappings() { # $1 = resource folder, $2 = "names" if the resource can also be fetched by name
  local resource="$1" by_name="$2" f
  for f in "$FILES/$resource"/*.json; do
    jq -c --argjson h "$HEADERS" --arg r "$resource" --arg file "$resource/$(basename "$f")" --arg n "$by_name" '{
      name: "\($r) \(.id)", priority: 5,
      request: {method: "GET",
                urlPathPattern: (if $n == "names" then "/api/v2/\($r)/(?i)(\(.id)|\(.name))/?"
                                 else "/api/v2/\($r)/\(.id)/?" end)},
      response: {status: 200, headers: $h, bodyFileName: $file}}' "$f"
  done | jq -s '{mappings: .}' > "$MAPPINGS/$resource.json"
}
resource_mappings pokemon names
resource_mappings pokemon-species names
resource_mappings evolution-chain ""

jq -n --argjson h "$HEADERS" --argjson n "$LAST_ID" '{mappings: [
  {name: "pokemon list page (templated slice)", priority: 5,
   request: {method: "GET", urlPath: "/api/v2/pokemon",
             queryParameters: {offset: {matches: "[0-9]{1,9}"}, limit: {matches: "[0-9]{1,3}"}}},
   response: {status: 200, headers: $h, bodyFileName: "pokemon-list/page.json.hbs",
              transformers: ["response-template"]}},
  {name: "pokemon list page far past the end", priority: 5,
   request: {method: "GET", urlPath: "/api/v2/pokemon",
             queryParameters: {offset: {matches: "[0-9]{10,}"}, limit: {matches: "[0-9]{1,3}"}}},
   response: {status: 200, headers: $h, jsonBody: {count: $n, next: null, previous: null, results: []}}}
]}' > "$MAPPINGS/pokemon-list.json"

# Anything not recorded (Pokémon 152+, unknown names, other resources): PokeAPI's real 404.
jq -n --argjson h "$HEADERS" '{mappings: [
  {name: "not recorded: PokeAPI 404", priority: 10,
   request: {method: "ANY", urlPattern: "/.*"},
   response: {status: 404, headers: $h, bodyFileName: "errors/not-found.json"}}
]}' > "$MAPPINGS/not-found.json"

echo "Done: $(find "$FILES/pokemon" -name '*.json' | wc -l | tr -d ' ') Pokémon," \
  "$(find "$FILES/evolution-chain" -name '*.json' | wc -l | tr -d ' ') evolution chains," \
  "$(du -sh "$HERE" | cut -f1) on disk"
