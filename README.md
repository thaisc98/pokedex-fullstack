# Pokédex — Full-Stack Clean Architecture Exercise

A RESTful API built with **Java 25 + Spring Boot 4.1.1**. It reads Pokémon data from [PokeAPI](https://pokeapi.co/docs/v2), lets users copy entries into a local PostgreSQL database and add their own fields to them, and protects writes with JWT authentication. A **React + TypeScript + Redux Toolkit** frontend uses the API.

The project follows **Clean Architecture** and **TDD**.

> **Status:** 🟡 Planning. Architecture, test strategy and roadmap are done; no application code yet. See [docs/roadmap.md](docs/roadmap.md) for the current phase.

---

## User stories

| ID | Story | Endpoint(s) |
|---|---|---|
| US01 | Browse Pokémon page by page: sprite, category, mass, skills | `GET /api/v1/pokemon` |
| US02 | Detail view: image, base stats, description, evolution chain | `GET /api/v1/pokemon/{idOrName}` |
| US03 | Copy a Pokémon into the local DB to add our own fields | `POST /api/v1/collection` |
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
| Testing (backend) | JUnit 5, AssertJ, jqwik, Mockito, WireMock, Testcontainers, RestAssured, ArchUnit, JaCoCo, PIT |
| Frontend | Node.js 26, Next.js 16 (App Router), React 19, TypeScript (strict), **HeroUI v3** (React Aria), Tailwind CSS v4, Redux Toolkit + RTK Query, React Hook Form + Zod, next-themes |
| Testing (frontend) | Vitest, React Testing Library, MSW, Playwright, axe-core, Lighthouse CI |
| Security scanning | OWASP Dependency-Check, `npm audit`, OWASP ZAP baseline scan, CycloneDX SBOM, Dependabot |
| Delivery | Docker (multi-stage), Docker Compose, GitHub Actions |

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
| [docs/adr/](docs/adr/) | Architecture Decision Records |
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

**Make a user an ADMIN** (there's no demo seed yet; needed to test delete). Register in the app first, then:

```bash
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "UPDATE app_user SET role = '"'"'ADMIN'"'"' WHERE email = '"'"'you@example.com'"'"';"'
```

Log out and in again so the new role is in your token.

**Postman:** import `postman/pokedex.postman_collection.json` and `postman/local.postman_environment.json`, select the **Pokédex local** environment, and run the folders in order (0 → 5). Cookies and CSRF are handled by the collection scripts. Folder 4 (ADMIN) needs the command above for the email shown in the collection variable `email`, then "Login again".

### 3. Run the tests

```bash
cd backend && ./mvnw verify
```
```bash
cd frontend && npm ci && npm test
```

The backend integration tests start PostgreSQL with Testcontainers, so Docker must be running.

### Demo credentials

> The demo seed data is added in Phase 6. Until then, register an account in the app and use the ADMIN command above.
