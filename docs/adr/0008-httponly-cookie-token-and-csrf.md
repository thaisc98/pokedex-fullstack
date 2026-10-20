# ADR 0008 — JWT sent in an httpOnly cookie, with CSRF protection

**Status:** Accepted · 2026-10-19 · *Changes how ADR 0005 delivers the token and how ADR 0006 stores it*

## Context
ADR 0005 issues a JWT, and the first frontend design kept it in `sessionStorage`. Any JavaScript on the page can read Web Storage, so a single XSS bug would let an attacker steal the token. The OWASP guidance (HTML5 Security, JWT for Java and Session Management cheat sheets) advises against keeping tokens in Web Storage.

## Decision
- On login, the backend sets the JWT as a cookie: `access_token; HttpOnly; Secure; SameSite=Strict; Path=/api; Max-Age=3600`.
- A custom `BearerTokenResolver` reads the token from the cookie. Validation itself is unchanged: OAuth2 Resource Server with `NimbusJwtDecoder`.
- Because the browser now sends the cookie automatically, **CSRF protection is turned on** with `http.csrf(csrf -> csrf.spa())`, Spring Security's built-in SPA setup (`@since 7.0`). It uses a JavaScript-readable `XSRF-TOKEN` cookie and the `SpaCsrfTokenRequestHandler`. Spring only creates the token when needed, so the SPA fetches it from `GET /auth/csrf` on startup and after login/logout. Full design: `docs/security.md` §4.2.1. A readable `XSRF-TOKEN` cookie is sent back as the `X-XSRF-TOKEN` header on every non-GET request, login included.
- **Same origin:** the Next.js server serves the pages and forwards `/api/*` to the Spring API with `rewrites()` (ADR 0010), the same in development and production. **No CORS** is configured.
- New endpoints: `POST /auth/logout` (clears the cookie) and `GET /auth/csrf` (returns a CSRF token).
- Frontend: the store keeps only `user` and `expiresAt`. The token is never visible to JavaScript. The listener middleware stores only the non-sensitive session hint.

## Consequences
- ✅ An XSS bug can't steal the token, which removes the biggest frontend risk.
- ✅ No CORS setup to get wrong.
- ✅ `SameSite=Strict` and the CSRF token protect against CSRF independently.
- ➖ More moving parts: the CSRF token flow has to be wired into `baseQuery` and covered by tests.
- ➖ Calling the API from tools like curl or Postman needs the cookie and CSRF header. Documented in the README.
- ℹ️ Browsers treat `http://localhost` as a secure context, so `Secure` cookies work in local development without TLS.

## Alternatives considered
- **Bearer token in `sessionStorage`:** simplest, but any XSS can read it.
- **Token in memory only:** safe from storage theft, but every page reload logs the user out, which is poor UX without a refresh-token flow.
- **Backend-for-Frontend with server sessions:** the strongest option, but it adds server-side session state and goes against the stateless design of ADR 0005.
