# Pokédex — Full-Stack Clean Architecture Exercise

A RESTful API built with **Java 25 + Spring Boot 4.1.1**. It reads Pokémon data from [PokeAPI v2](https://pokeapi.co/docs/v2), lets users copy entries into a local PostgreSQL database and add their own fields to them, and protects writes with JWT authentication. A **Next.js 16 + React 19 + HeroUI v3 + Redux Toolkit** frontend uses the API.

The project follows **Clean Architecture** and **TDD**.

> **Status:** 🟢 All four user stories and authentication work end to end (12 API endpoints, catalog and collection UI, Docker, offline demo). 🟡 Playwright E2E, Lighthouse and the ZAP scan are still to come. See [docs/roadmap.md](docs/roadmap.md).

**Quick start:** `cp .env.example .env`, set `JWT_SECRET` (`openssl rand -base64 48`), then `docker compose up -d --build` and open http://localhost:3000. Log in with the `DEMO_*` accounts from your `.env` (see [Demo credentials](#demo-credentials)). No internet? See [Offline demo](#offline-demo-no-internet).

---

## User stories

| ID | Story | Endpoint(s) |
|---|---|---|
| US01 | Browse Pokémon page by page: sprite, category, mass, skills | `GET /api/v1/pokemon` |
| US02 | Detail view: image, base stats, description, evolution chain | `GET /api/v1/pokemon/{idOrName}` |
| US03 | Copy a Pokémon into the local DB to add our own fields | `POST /api/v1/collection` (+ `GET` list/one, admin `DELETE`) |
| US04 | Update a locally stored Pokémon, with validation | `PUT /api/v1/collection/{pokedexId}` |

## Tech stack

| Layer | Technology |
|---|---|
| Language / runtime | Java 25 (LTS), virtual threads |
| Framework | Spring Boot 4.1.1 (Spring Framework 7, Jakarta EE 11, Jackson 3) |
| Build | Maven 3.9 (wrapper), multi-module build |
| Persistence | PostgreSQL 17, Spring Data JPA, Flyway |
| Caching | Caffeine (Spring Cache) |
| Security | Spring Security 7, OAuth2 Resource Server (JWT in an httpOnly cookie + CSRF), BCrypt, Bucket4j rate limiting; OWASP Top 10:2025 / ASVS 5.0 L1 + selected L2 |
| Testing (backend) | JUnit 5, AssertJ, Mockito, WireMock, Testcontainers, RestAssured, ArchUnit, JaCoCo (per-module gates), PIT (mutation) |
| Frontend | Node.js 26, Next.js 16 (App Router), React 19, TypeScript (strict), **HeroUI v3** (React Aria), Tailwind CSS v4, Redux Toolkit + RTK Query, React Hook Form + Zod, next-themes |
| Testing (frontend) | Vitest, React Testing Library, MSW; Playwright + axe-core (in progress), Lighthouse (planned) |
| Security scanning | gitleaks, OWASP Dependency-Check (weekly), `npm audit` (production deps), CycloneDX SBOM, Dependabot; OWASP ZAP baseline (on hold) |
| Delivery | Docker (multi-stage, non-root, pinned by digest), Docker Compose (postgres → backend → frontend → nginx edge), WireMock offline mode, GitHub Actions |

## Repository layout

```
pokedex/
├── backend/        # Maven multi-module build (domain → application → infrastructure/presentation → bootstrap)
├── frontend/       # Next.js 16 App Router + HeroUI v3 + Redux Toolkit
├── docs/           # Architecture, ADRs, security, UI/UX, testing strategy, roadmap, agents, GenAI docs
├── .claude/agents/ # Agent definitions: backend-engineer, frontend-engineer, test-engineer
└── docker-compose.yml
```

## Documentation

| Document | What it covers |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Domain model, layer boundaries, ports, module layout, schema, API contract |
| [docs/security.md](docs/security.md) | Threat model, access levels, Spring Boot + React controls, OWASP Top 10 / API Top 10 / ASVS mapping |
| [docs/ui-ux.md](docs/ui-ux.md) | Design principles, user flows, wireframes, design system, accessibility (WCAG 2.2 AA), performance budgets |
| [docs/testing-strategy.md](docs/testing-strategy.md) | TDD approach, test pyramid, coverage gates, edge cases |
| [docs/roadmap.md](docs/roadmap.md) | Phases with entry/exit criteria, risk register |
| [docs/requirements-checklist.md](docs/requirements-checklist.md) | Every exercise requirement mapped to where it is met |
| [docs/adr/](docs/adr/) | Architecture Decision Records (0001–0014) |
| [docs/api/openapi.yaml](docs/api/openapi.yaml) | The API contract (Swagger UI at http://localhost:8080/swagger-ui with the `demo` profile) |
| [docs/quality/](docs/quality/) | Quality reports per story: acceptance criteria, coverage, mutation score, findings |
| [docs/demo-script.md](docs/demo-script.md) | Step-by-step script for the live demo |
| [postman/](postman/) | Postman collection for all 12 endpoints (cookies and CSRF handled automatically) |
| [docs/agents/README.md](docs/agents/README.md) | AI agent team: backend, frontend and test engineers; file ownership, workflow, handoff notes |
| [docs/genai/workflow.md](docs/genai/workflow.md) | How GenAI is used and the review checklist for AI-generated code |
| [docs/genai/prompt-log.md](docs/genai/prompt-log.md) | Log of prompts and the decision taken on each output |
| [docs/genai/task-management-exercise.md](docs/genai/task-management-exercise.md) | The separate GenAI exercise (task-management API) |

## Getting started

### 1. Environment variables

Secrets are never committed. Copy the templates and fill them in:

```bash
cp .env.example .env
```
```bash
cp frontend/.env.example frontend/.env.local
```

Set `JWT_SECRET` in `.env` to at least 256 bits of random data:

```bash
openssl rand -base64 32
```

### 2. Run the whole stack (Docker)

Needs Docker Desktop running. From the repo root, with `.env` filled in (step 1):

```bash
docker compose up -d --build
```

| What | URL |
|---|---|
| Web app (through the nginx edge proxy) | http://localhost:3000 |
| API docs / Swagger UI (`demo` profile) | http://localhost:8080/swagger-ui |
| API base (direct, for curl) | http://localhost:8080/api/v1 |

PostgreSQL starts first, then the backend (Flyway creates the tables on start), then the frontend, then the `edge` nginx proxy that serves it on port 3000. The first build takes a few minutes. Follow the logs, or stop everything:

```bash
docker compose logs -f backend
```
```bash
docker compose down
```

Add `-v` to `down` to also delete the database volume. Ports are bound to `127.0.0.1` only; change them with `FRONTEND_PORT`, `BACKEND_PORT` and `POSTGRES_PORT` in `.env`.

> Use Chrome or Firefox at `http://localhost:3000`. The login cookie is `Secure`; browsers accept that on `localhost` over plain HTTP.

**Make a user an ADMIN** (only needed without the demo seed below; delete requires ADMIN). Register in the app first, then:

```bash
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "UPDATE app_user SET role = '"'"'ADMIN'"'"' WHERE email = '"'"'you@example.com'"'"';"'
```

Log out and in again so the new role is in your token.

**Postman:** import `postman/pokedex.postman_collection.json` and `postman/local.postman_environment.json`, select the **Pokédex local** environment, and run the folders in order (0 → 5). Cookies and CSRF are handled by the collection scripts. Folder 4 (ADMIN) needs the command above for the email shown in the collection variable `email`, then "Login again".

### Offline demo (no internet)

The same stack with PokeAPI replaced by a local WireMock server (`pokeapi-mock`) that serves recorded **Generation 1** data (Bulbasaur #1 to Mew #151, plus every Pokémon in their evolution chains such as Pichu, Crobat or Sylveon). Same `.env` as above; add the override file to every command:

```bash
docker compose -f docker-compose.yml -f docker-compose.offline.yml up -d --build
```
```bash
docker compose -f docker-compose.yml -f docker-compose.offline.yml down -v
```

The images must already be pulled or built once (that first build needs internet; after that, none). What changes in offline mode:

- The catalog has 151 Pokémon. Any other id or name (for example #152) answers 404, as PokeAPI does for an unknown Pokémon.
- The backend is on an internal-only Docker network, so it cannot reach the internet even by mistake. Its port 8080 is not published: use the API at `http://localhost:3000/api/v1` (through the edge proxy). Swagger UI is not reachable in this mode.
- Sprites and artwork are loaded by your browser from `raw.githubusercontent.com`. With no internet they don't load and the UI shows its image fallback.
- The demo seed (accounts and the 6-Pokémon collection) works as usual.

The recorded data, the script that records it, and the transformations applied are described in [docker/pokeapi-mock/README.md](docker/pokeapi-mock/README.md).

### 3. Run the tests

```bash
cd backend && ./mvnw verify
```
```bash
cd frontend && npm ci && npm test
```

The backend integration tests start PostgreSQL with Testcontainers, so Docker must be running.

### Demo credentials

With the `demo` profile (the default in `.env.example`), the backend seeds demo data on startup:

- **Two accounts** from your `.env`: an ADMIN (`DEMO_ADMIN_EMAIL` / `DEMO_ADMIN_PASSWORD`) and a USER (`DEMO_USER_EMAIL` / `DEMO_USER_PASSWORD`). Log in with the values you set there. The passwords follow the normal policy (12–64 characters, not a common password); an account whose variable is missing or too weak is skipped with a warning in the backend log.
- **A sample collection:** Bulbasaur, Charmander, Squirtle, Pikachu, Eevee and Snorlax, synced from PokeAPI, with a Japanese name, the Kanto region, a habitat and tags such as `starter`. If PokeAPI is unreachable, the collection is skipped and the app still starts.

The seed is idempotent: existing accounts (and their passwords) and Pokémon already in the collection are left as they are. Without any `DEMO_*` variable nothing is seeded. Change a demo password by editing it in the app's database or by using a new `DEMO_*_EMAIL`.
