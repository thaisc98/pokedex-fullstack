# Demo Script (≈ 12 minutes)

> Related: [README](../README.md) · [requirements-checklist.md](requirements-checklist.md) · [genai/prompt-log.md](genai/prompt-log.md)

## Before the demo (10 minutes earlier)

1. Docker Desktop running. In the repo root, `.env` with `JWT_SECRET` and the four `DEMO_*` values.
2. Start the stack. Use the **offline** mode if the room's network is unreliable (Gen 1 only, images show the fallback):
   ```bash
   docker compose up -d --build
   ```
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.offline.yml up -d --build
   ```
3. `docker compose ps` → all services `healthy`. Open http://localhost:3000 and http://localhost:8080/swagger-ui (online mode only).
4. Open a private window too (for the second user), and the GitHub repo (Actions tab, a merged PR) in another tab.

## 1. The problem and the shape of the solution (2 min)
- User stories US01–US04 (README table) and the two halves: a **read-only Catalog** from PokeAPI and a **Collection** we own (ADR 0002).
- Clean Architecture: `domain → application → infrastructure / presentation → bootstrap`, enforced by the Maven enforcer and ArchUnit (ADR 0001). Show the module list in `backend/`.

## 2. Catalog: US01 and US02 (2 min)
- `/pokemon`: the card grid (sprite, category, mass in kg, abilities with the hidden one labelled), pagination in the URL, Back works. Resize to phone width: 2 columns, mobile menu.
- Open **Eevee**: stats, description, abilities, the evolution tree with all 8 branches; click Jolteon.
- Mention: ~41 PokeAPI calls per page, fetched in parallel (virtual threads, concurrency limit 10) and cached per resource (ADR 0004).
- `/pokemon/does-not-exist` → friendly not-found page.

## 3. Collection: US03 and US04 (3 min)
- Signed out, press **Add to collection** on Pikachu → redirected to login → back on Pikachu after signing in as the demo **USER**.
- Add it → toast "Added". Press again → "Refreshed from PokeAPI; your custom fields were kept" (BR-05).
- `/collection`: the seeded Pokémon, search by name, filter by tag.
- Edit Pikachu: add a localized name, region, coordinates, tags (`" Starter "` → `starter`). Try latitude **91** → error summary gets focus (accessibility). Save.
- Conflict: edit the same record in two tabs, save one, then the other → **"Reload latest"** dialog (optimistic locking, 409).
- As USER: no Delete action. Log in as **ADMIN** in the private window: Delete asks for confirmation; the record disappears.

## 4. Security, briefly (2 min)
- JWT in an **HttpOnly** cookie + CSRF on every write (ADR 0008). DevTools → Application → Cookies: `access_token` is HttpOnly; `XSRF-TOKEN` is readable.
- Deny by default; the **authorization-matrix test** fails the build for any endpoint without a row.
- Story worth telling: Spring's resource-server DSL silently disabled CSRF for cookie tokens; an acceptance test caught it (prompt log #008).
- Rate limiting per real client IP through the nginx edge proxy (ADR 0011); least-privilege DB user; secrets only from `.env`.

## 5. Quality and how it was built (2 min)
- GitHub Actions on a PR: backend `verify` (unit + Testcontainers ITs, coverage gates, enforcer, ArchUnit, Spotless), frontend lint/typecheck/test/build, `npm audit`, gitleaks.
- Coverage and PIT numbers from [docs/quality/](quality/). The `test:` → `feat:` commit pairs in the history.
- Contract-first: `docs/api/openapi.yaml`; the springdoc contract IT fails if the code drifts.

## 6. GenAI (1 min, then the GenAI section)
- Three agents with file ownership (the tester owns the acceptance tests; developers may not edit them) and a lead session reviewing every step: [agents/README.md](agents/README.md).
- Show two prompt-log rows where the AI was wrong and the process caught it (#007 harness bug, #008 CSRF).

## If something goes wrong
- **PokeAPI slow or down:** switch to the offline command above (restart takes ~1 minute).
- **Login fails after a reset:** `docker compose down -v`, then up again (re-creates the DB role and the demo seed).
- **Port in use:** set `FRONTEND_PORT`, `BACKEND_PORT` or `POSTGRES_PORT` in `.env`.
