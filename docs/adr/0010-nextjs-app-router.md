# ADR 0010 — Next.js 16 (App Router) instead of Vite + React Router

**Status:** Accepted · 2026-10-19 · *Replaces ADR 0006's framework choice (Vite + React Router). The Redux Toolkit decision in ADR 0006 still holds.*

## Context
The frontend has public pages (catalog, detail), protected pages (collection, edit) and auth pages. With Vite + React Router we would wire up routing, layouts, loading/error states, route protection and the `/api` proxy ourselves. Next.js has all of these built in, and its file-based routes make the structure obvious to reviewers.

Facts checked in the Next.js 16.3 docs bundled with the package (`node_modules/next/dist/docs/`) on 2026-10-19:
- `middleware.ts` is now called **`proxy.ts`** (renamed in 16; same functionality). Proxy is meant for optimistic checks, **not** as a full authorization solution.
- `params` and `searchParams` are **Promises** in pages and layouts.
- CSP nonces are generated in `proxy.ts` and need **dynamic rendering**. Next applies the nonce to its own scripts automatically.
- Vitest **does not support async Server Components**. Those are covered by E2E tests.
- `output: 'standalone'` produces a small self-contained server for Docker.

## Decision
- **Next.js 16 (App Router)**, React 19, TypeScript strict, Tailwind v4, HeroUI v3 (ADR 0009). The exact 16.x version is pinned in P0.
- **Rendering split:**
  - **Catalog pages (US01/US02) are Server Components.** They fetch from the Spring API on the server through a `server-only` client (`BACKEND_INTERNAL_URL`). The HTML arrives already rendered: fast first paint, no loading spinner, nothing needed in the client-side cache.
  - **Collection and auth (US03/US04) are Client Components** using **Redux Toolkit / RTK Query**. These need mutations, cache invalidation, forms and optimistic UX.
  - The "Add to collection" button on the detail page is a small Client Component inside the server-rendered page.
- **Routes:** file-based under `app/`, with route groups `(public)`, `(auth)`, `(protected)`. Built-in `loading.tsx` (skeletons), `error.tsx` (error state + retry) and `not-found.tsx` (404).
- **Same origin:** `next.config.ts` `rewrites()` forward `/api/:path*` to the Spring API. The browser only ever talks to the Next server, so ADR 0008 (httpOnly cookie, CSRF, no CORS) is unchanged.
- **`proxy.ts`:** (1) generates a per-request CSP nonce and sets the security headers, (2) **optimistic** redirect of `/collection/**` to `/login?from=` when the `access_token` cookie is absent. The Spring API remains the only place where authorization is enforced.
- **Redux store per request:** `makeStore()` called inside a `StoreProvider` Client Component (`useRef`), following the RTK Next.js guide. **No module-level store**, because on the server it would be shared between users.
- **Theme:** `next-themes` (HeroUI's documented Next.js approach) with `suppressHydrationWarning` on `<html>`. Its inline anti-flash script receives the CSP nonce.
- **Images:** `next/image` with `images.remotePatterns` restricted to the PokeAPI sprites path on `raw.githubusercontent.com`. Images are served from our own origin, so CSP can use `img-src 'self' data:`.
- **Node.js 26** everywhere: local development (`.nvmrc`), CI and the Docker image. Node 26 is the newest major line and becomes **LTS on 2026-10-28** (Node.js release schedule), supported until 2029-04-30.
- **Docker:** `output: 'standalone'` on a `node:26-alpine` image, non-root user.

## Consequences
- ✅ Routing, layouts, loading/error/404 states and code splitting with no extra libraries.
- ✅ Server-rendered catalog pages: better LCP and no loading spinners for the pages most people see.
- ✅ `proxy.ts` gives one place for security headers, the CSP nonce and redirect checks.
- ➖ Two ways of fetching data (Server Components for the catalog, RTK Query for the collection). The rule is simple: **public read-only data → server; authenticated or changing data → RTK Query.**
- ➖ A nonce-based CSP makes every page dynamically rendered (no static HTML). Acceptable: the backend cache keeps the catalog fast.
- ➖ Async Server Components can't be unit-tested with Vitest, so they are covered by Playwright.
- ⚠️ **AI tools often generate outdated Next.js code:** Pages Router (`pages/`, `getServerSideProps`, `next/router`), `middleware.ts`, synchronous `params`. Listed in `AGENTS.md`.
- ⚠️ Lesson from CVE-2025-29927 (middleware bypass): never rely on `proxy.ts` for authorization.

## Alternatives considered
- **Vite + React Router (ADR 0006's original choice):** lighter, but routing, layouts and route protection would all be hand-built.
- **Everything as Client Components with RTK Query:** one data-fetching approach, but we'd lose the main benefit of Next (server-rendered public pages), and reviewers would ask "why Next, then?".
- **Remix / React Router framework mode:** comparable, but Next has wider adoption and first-class HeroUI documentation.
