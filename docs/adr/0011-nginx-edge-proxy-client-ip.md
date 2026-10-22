# ADR 0011 — nginx edge proxy decides the client IP

**Status:** Accepted · 2026-10-21 · PR #35 (builds on #31)

## Context
Login and registration are rate-limited per client IP (security.md §4.1, Bucket4j). Behind the Next.js `/api` rewrite every request reaches Spring from the Next.js server, so #31 made Spring trust `X-Forwarded-For` from private-network proxies (`TRUSTED_PROXIES`). Testing showed that Next.js **passes a client-supplied `X-Forwarded-For` through unchanged** (it only fills the header when it is missing) and Next.js 16 doesn't expose the socket address to `proxy.ts`. A browser could therefore send a new fake IP with every request and bypass the limits.

## Decision
- An **`edge`** service (`nginxinc/nginx-unprivileged`, pinned by digest, non-root) is the only published entry to the web app. It **overwrites** `X-Forwarded-For` with `$remote_addr` (never `$proxy_add_x_forwarded_for`) and sets `X-Forwarded-Proto/Host`.
- The `frontend` container publishes no port (`expose` only).
- Spring keeps trusting `X-Forwarded-For` only from `TRUSTED_PROXIES` (default: private and loopback ranges).

## Consequences
- ✅ Verified: five registrations with five different fake `X-Forwarded-For` values share one bucket (3 × 201, then 429).
- ✅ Standard production shape; TLS termination can be added at the same place later.
- ➖ One more container.
- ➖ The backend port published on `127.0.0.1` for manual testing reaches Spring from the Docker gateway (a trusted private address), so it must never be exposed beyond localhost.

## Alternatives considered
- **Accept the risk for the demo:** rejected by the human (option B).
- **Rate-limit on the left-most / right-most `X-Forwarded-For` entry in Spring:** fragile; still spoofable without a component that owns the header.
