# ADR 0002 — Read-only Catalog kept separate from the local Collection

**Status:** Accepted · 2026-10-19

## Context
US01/US02 browse data owned by PokeAPI. US03/US04 copy Pokémon into our database so we can add our own fields (localized names, geographic metadata, tags) and edit them. If both live in one resource, it becomes unclear which source is authoritative and which fields can be changed.

## Decision
Two resources:
- **Catalog** (`/api/v1/pokemon`): read-only, data comes from PokeAPI through `PokemonCatalogPort`, public.
- **Collection** (`/api/v1/collection`): local `CollectedPokemon` aggregate, full CRUD, authenticated.

A `CollectedPokemon` contains a read-only `UpstreamSnapshot` and editable `CustomAttributes`. A re-sync refreshes the snapshot and never overwrites the custom attributes. US04 can change only the custom attributes; any attempt to change upstream fields returns 400.

## Consequences
- ✅ A clear owner for every field, which makes the update rules easy to explain and test.
- ✅ The Collection can be demoed fully offline from seeded data.
- ➖ Two similar DTO families (catalog vs collection).

## Alternatives considered
- **Copy everything locally and serve all reads from the DB:** needs a full background sync job and tends to go stale.
- **Let US04 edit upstream fields too:** our data would drift from PokeAPI, and a re-sync could not merge changes cleanly.
