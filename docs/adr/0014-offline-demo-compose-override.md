# ADR 0014 — Offline demo through a Compose override with a WireMock PokeAPI

**Status:** Accepted · 2026-10-22 · PR #37

## Context
The roadmap's top risk is PokeAPI being slow or down during the live demo; P6 requires the stack to work with no internet. The plan said "`docker compose --profile offline`".

## Decision
- `docker-compose.offline.yml` adds a `pokeapi-mock` service (`wiremock/wiremock`, pinned by digest, non-root, read-only) and points the backend at it (`POKEAPI_BASE_URL`). The backend waits for the mock and is attached only to an **internal network** with no route to the internet.
- Run with `docker compose -f docker-compose.yml -f docker-compose.offline.yml up -d --build`.
- The mock serves **real recorded responses for Generation 1** (ids 1–151) plus every species their evolution chains reference (188 Pokémon, 78 chains, ~2.1 MB). A committed script (`docker/pokeapi-mock/record-offline-pokeapi.sh`) re-records them and strips fields the backend never reads. The list endpoint is one templated mapping that slices the recorded list for any page size.

## Consequences
- ✅ The demo works with no internet after the first build; the backend provably can't reach pokeapi.co in this mode.
- ➖ Only 151 Pokémon offline; sprites and artwork (loaded by the browser from GitHub) show the fallback image.
- ➖ The backend port isn't published in offline mode (internal network), so Swagger UI isn't available; use the app on port 3000.

## Alternatives considered
- **`--profile offline` only:** a profile can add a service but can't change the backend's environment or startup order.
- **Edit `POKEAPI_BASE_URL` in `.env`:** easy to forget to undo, and the backend wouldn't wait for the mock.
- **A Spring `offline` profile with an in-process mock:** couples test infrastructure to production code.
