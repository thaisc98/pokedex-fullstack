# ADR 0005 — Stateless JWT via Spring OAuth2 Resource Server

**Status:** Accepted · 2026-10-19 · *How the token is delivered was changed by [ADR 0008](0008-httponly-cookie-token-and-csrf.md)*

## Context
The exercise requires an auxiliary API for user registration and authentication, with public and protected routes. The client is a single-page app.

## Decision
- `POST /auth/login` issues a signed JWT (HS256, ≥ 256-bit secret from an environment variable, checked at startup; 1 h expiry; `iss`/`aud` claims) through Spring's `JwtEncoder`, and delivers it in an httpOnly cookie (ADR 0008). `POST /auth/register` creates the account.
- Protected routes are validated by `spring-boot-starter-oauth2-resource-server` (`JwtDecoder`). No hand-written filter.
- Passwords are hashed with BCrypt (strength 12) via `DelegatingPasswordEncoder`, behind the `PasswordHasher` port. Policy: 12–64 characters, no composition rules, common-password list check (see `docs/security.md`).
- Roles: `USER` (collection read/sync/update) and `ADMIN` (delete as well).
- Login returns the same 401 for an unknown email and for a wrong password, and both take similar time. Login is rate-limited (Bucket4j).

## Consequences
- ✅ No hand-written JWT parsing or crypto.
- ✅ Stateless, so it's easy to test with `spring-security-test` JWT helpers.
- ➖ There is no way to revoke a token before it expires. A refresh-token flow is out of scope.

## Alternatives considered
- **Server-side session cookies:** need server-side session state. (We still use a cookie to carry the JWT; see ADR 0008.)
- **External identity provider (Keycloak):** too much for a demo; it adds infrastructure.
