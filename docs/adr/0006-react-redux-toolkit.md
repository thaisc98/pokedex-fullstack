# ADR 0006 — React + TypeScript + Redux Toolkit (RTK Query)

**Status:** Accepted · 2026-10-19 · *Framework (Vite + React Router) replaced by [ADR 0010](0010-nextjs-app-router.md) (Next.js); token storage changed by [ADR 0008](0008-httponly-cookie-token-and-csrf.md). The Redux Toolkit decision stands, used in Client Components.*

## Context
The exercise requires a modern frontend with a responsive, user-centric design, clean component organization and **efficient state management**, and asks for no browser console warnings.

## Decision
- React + TypeScript (strict). *(Framework: Next.js, see ADR 0010.)*
- **Redux Toolkit** as the single store:
  - **RTK Query** (`createApi` + `injectEndpoints`) for all server data, with cache tags to refetch after changes.
  - `createSlice` for app state (`authSlice` holds `user` + `expiresAt` only; **the token is never in JavaScript**, see ADR 0008).
  - `createListenerMiddleware` for session side effects (expiry warning, clearing the cache on logout).
- Page number and filters live in the URL (`searchParams`).
- Under Next.js, the store is created **per request** with `makeStore()` in a `StoreProvider` Client Component, never as a module-level singleton (ADR 0010). Public catalog pages are Server Components and don't use the store. Form input lives in React Hook Form + Zod.

## Consequences
- ✅ One clear rule: server data goes in RTK Query, app state in slices, short-lived state in the URL or forms.
- ✅ Redux DevTools makes the state easy to show in the demo.
- ➖ More setup than plain hooks. Mitigated by typed hooks (`.withTypes()`) and a `makeStore()` factory.
- ⚠️ RTK's development checks warn about non-serializable values, so the store holds only plain JSON (ISO date strings).

## Alternatives considered
- **TanStack Query:** good for server data, but rejected in favour of a single Redux Toolkit store.
- **RTK with hand-written `createAsyncThunk`s:** we would have to write caching, de-duplication and refetching ourselves.
