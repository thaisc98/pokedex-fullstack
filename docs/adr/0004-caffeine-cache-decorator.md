# ADR 0004 — Caffeine cache in a decorator around the PokeAPI adapter

**Status:** Accepted · 2026-10-19 · Collection sync behaviour refined by [ADR 0013](0013-collection-sync-live-and-cache-refresh.md)

## Context
One list page needs about 41 PokeAPI calls (see architecture §1.5). PokeAPI data almost never changes, and its fair-use policy asks clients to cache. The exercise lists caching as a nice-to-have.

## Decision
- `CachingPokemonCatalog` implements `PokemonCatalogPort` and wraps `PokeApiCatalogAdapter`.
- Caffeine, with a size limit; TTL 24 h for successful lookups; not-found results are not cached (or for at most 5 min).
- Each underlying resource (`/pokemon/{id}`, `/pokemon-species/{id}`, `/evolution-chain/{id}`) is cached separately. An evolution chain is shared by several Pokémon, so caching it once avoids repeat calls.
- The API also sends `Cache-Control` headers on catalog responses.

## Consequences
- ✅ Use cases don't know caching exists. The decorator can be tested on its own.
- ✅ Repeat page loads make no calls to PokeAPI.
- ➖ The cache is per instance. Fine for this scope; Redis would be the next step if we ran several instances.

## Alternatives considered
- **`@Cacheable` on the adapter:** less code, but it hides the behaviour behind a proxy and is harder to unit-test.
- **Redis:** not needed for a single instance.
