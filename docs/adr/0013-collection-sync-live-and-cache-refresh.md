# ADR 0013 — Collection sync reads PokeAPI live and refreshes the catalog cache

**Status:** Accepted · 2026-10-21 · PRs #27, #33 · refines ADR 0002 and ADR 0004

## Context
US03 copies a Pokémon into the local collection; a re-sync must refresh the upstream snapshot (BR-05). The catalog reads through the Caffeine decorator from ADR 0004 (24 h TTL), so a sync through the same port could store data up to a day old.

## Decision
- The collection has its own outbound port, `UpstreamSnapshotPort`, implemented by an adapter on the **uncached** PokeAPI gateway: a sync always reads PokeAPI live. (Architecture §2.2 had put `fetchSnapshot` on `PokemonCatalogPort`.)
- After a successful live fetch, the fresh `/pokemon/{id}` and `/pokemon-species/{id}` resources are written into the per-resource caches, and the **page cache is cleared**, so the catalog never shows older data than the collection.
- All HTTP calls happen before the database transaction.
- The collection's read side uses separate `ListCollectedPokemon` and `GetCollectedPokemon` use cases instead of one `QueryCollectionUseCase`.

## Consequences
- ✅ A user who presses "Sync" always gets current data, and the catalog agrees with it.
- ➖ Every sync empties the page cache; the next list request rebuilds its page, mostly from the still-cached per-resource entries, so it stays cheap. Syncs are rare.

## Alternatives considered
- **Sync through the cached catalog port:** simpler, but can store stale data for up to 24 h.
- **Evict only the pages that contain the id:** precise but needs a reverse index; not worth it at this scale.
