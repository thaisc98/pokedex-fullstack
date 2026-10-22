# ADR 0012 — `pokedex_signed_in` hint cookie for the optimistic route guard

**Status:** Accepted · 2026-10-21 · PR #34 · refines security.md §3.3 and ADR 0008

## Context
security.md §3.3 says `proxy.ts` redirects signed-out visitors away from `/collection` by checking for the `access_token` cookie. ADR 0008 scopes that cookie to `Path=/api`, so browsers never send it with page requests: `proxy.ts` can't see it.

## Decision
After a successful login the frontend sets `pokedex_signed_in=1` (not HttpOnly, no secret, `Path=/`). It is removed on logout and on any 401. `proxy.ts` redirects `/collection*` to `/login?from=…` when the hint is missing, and the app skips `GET /auth/me` for visitors without it.

## Consequences
- ✅ The real token stays `HttpOnly` and scoped to `/api` (ADR 0008 unchanged).
- ✅ Visitors don't trigger a 401 on every page load.
- ➖ The hint can be stale (for example after a backend restart with a new `JWT_SECRET`); the first `/auth/me` then returns one 401 and the hint is cleared.
- **Security impact: none.** The hint grants nothing; every API call is still authorized by Spring (security.md §3.3: the proxy is never the security boundary).

## Alternatives considered
- **`access_token` with `Path=/`:** sends the JWT with every page and asset request; rejected (wider exposure for no security gain).
- **No server-side redirect, client-side only:** works, but flashes protected UI before redirecting.
