<!-- BEGIN:nextjs-agent-rules -->

## This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

# Frontend Rules (Next.js 16 · React 19 · HeroUI v3 · Redux Toolkit)

Applies to everything under `frontend/`. The root [AGENTS.md](../AGENTS.md) rules apply too. Agent role: [.claude/agents/frontend-engineer.md](../.claude/agents/frontend-engineer.md). The managed block above is written by `next dev`; keep it as it is.

## Layout

| Path | Contains |
|---|---|
| `src/proxy.ts` | Per-request CSP nonce + security headers; **optimistic** `/collection` redirect (never authorization) |
| `src/app/` | Routes: `(public)/pokemon` (Server Components), `(auth)/login`, `(auth)/register`, `(protected)/collection` (Client Components); `layout.tsx`, `loading.tsx`, `error.tsx`, `not-found.tsx` |
| `src/server/` | `'server-only'` code: `catalogClient` (talks to `BACKEND_INTERNAL_URL`) |
| `src/store/` | `makeStore`, `StoreProvider` (`'use client'`, `useRef`), typed hooks, listeners |
| `src/shared/api/` | `baseApi` (RTK Query), `baseQuery` (cookies, `X-XSRF-TOKEN`, ProblemDetail, 401/403 handling), generated `schema.d.ts` |
| `src/shared/ui/` | `AppLink`, `AppShell`, `EmptyState`, `ErrorState`, `ErrorSummary` |
| `src/shared/lib/` | `formatName`, `isSafeHttpUrl`, `safeRedirect`, problem-type → copy map |
| `src/features/<feature>/` | `catalog`, `collection`, `auth`: components, `*Api.ts`, slices, `*.test.tsx` next to the code |
| `src/mocks/` | MSW handlers built from the contract |
| `src/styles/themes/pokedex.css` | HeroUI variable overrides + Pokémon type colors |
| `e2e/` | Playwright specs. **Owned by the test engineer; don't edit** |

## Commands

```bash
npm run dev
```
```bash
npm run lint && npm run typecheck && npm test
```
```bash
npm run build
```
```bash
npm run gen:api
```
```bash
npm run test:e2e
```

## Conventions

- **Server vs client:** public read-only data → Server Component; authenticated or changing data → Client Component + RTK Query. Add `'use client'` only at the smallest interactive component.
- **Next.js 16:** `proxy.ts` (not `middleware.ts`); `await props.params` / `await props.searchParams`; `generateMetadata` for titles; `notFound()` for 404s.
- **HeroUI v3:** use the component from `docs/ui-ux.md` §6.4 before building your own; React Aria props (`onPress`, `isDisabled`, `isInvalid`); compound parts; internal links via `AppLink`.
- **Styling:** Tailwind v4 utilities built on HeroUI tokens (`bg-surface`, `text-muted`, `bg-type-fire`); no hardcoded colors; no inline `<style>`.
- **State:** RTK Query for collection/auth data; `authSlice` holds `user` + `expiresAt` only; HeroUI `toast()` for notifications; form input in React Hook Form + Zod; page number and filters in the URL.
- **Security:** never read or store the token; `credentials: 'same-origin'`; `X-XSRF-TOKEN` on non-GET requests; no `dangerouslySetInnerHTML`; `NEXT_PUBLIC_*` is public; `next/image` only for allow-listed sprite URLs.
- **Accessibility:** WCAG 2.2 AA; labels on every field; loading/empty/error/success states for every async view.
- **Tests:** Vitest + RTL + `@testing-library/user-event` + MSW; fresh `makeStore()` per test; async Server Components are covered by Playwright, not Vitest.

## Environment

- `BACKEND_INTERNAL_URL`: server-side only (rewrites + `catalogClient`), e.g. `http://backend:8080`.
- No `NEXT_PUBLIC_` secrets. There are none to add.
