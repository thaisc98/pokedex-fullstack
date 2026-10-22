# Requirements Checklist

Every requirement from the exercise brief, mapped to where it is designed and where it is built. Tick the **Done** box when the phase that delivers it passes its exit criteria.

## Functional requirements

| Requirement | Designed in | Delivered in | Done |
|---|---|---|---|
| Spring Boot REST API that calls PokeAPI | [architecture §1.5, §2.5](architecture.md) | P3, P4 | [x] |
| **US01** List Pokémon page by page: sprite, category, mass, skills | [architecture §1.4 UC-01](architecture.md) | P1–P5 | [x] |
| US01 nice-to-have: cache responses | [ADR 0004](adr/0004-caffeine-cache-decorator.md) | P3 | [x] |
| **US02** Detail view: image, stats, description, evolution chain | [architecture §1.4 UC-02](architecture.md) | P1–P5 | [x] |
| **US03** Copy Pokémon into the local relational DB | [ADR 0002](adr/0002-catalog-vs-collection-split.md), UC-03 | P1–P5 | [x] |
| US03 Our own fields: localized names, geographic metadata, tags | [architecture §1.2 CustomAttributes](architecture.md) | P1, P3 | [x] |
| **US04** Update a locally stored Pokémon | UC-04, BR-06 | P1–P5 | [x] |
| US04 404 for missing records, 400 for malformed input, more defensive checks | [testing-strategy §5](testing-strategy.md) | P4 | [x] |

## Technical requirements

| Requirement | Designed in | Delivered in | Done |
|---|---|---|---|
| Code in a public Git repository | — | P0 | [x] |
| Tests included | [testing-strategy](testing-strategy.md) | P1–P5 | [x] |
| Proper error handling | ProblemDetail, [architecture §2.8](architecture.md) | P4 | [x] |
| Nice-to-have: cache PokeAPI responses | [ADR 0004](adr/0004-caffeine-cache-decorator.md) | P3 | [x] |
| Frontend that uses the API | [ADR 0006](adr/0006-react-redux-toolkit.md) | P5 | [x] |
| Relational DB: a primary entity + a user table | [architecture §2.7](architecture.md) | P3 | [x] |
| Records have a unique primary key and ≥ 2 descriptive attributes | [architecture §2.7](architecture.md) | P3 | [x] |
| Full CRUD API using standard HTTP verbs and consistent responses | [architecture §2.8](architecture.md) | P4 | [x] |
| Auth API: registration, login, public vs protected routes | [ADR 0005](adr/0005-jwt-oauth2-resource-server.md) | P3, P4 | [x] |
| Separate data access layer | [architecture §2.2](architecture.md) (repository ports + JPA adapters) | P3 | [x] |
| Business logic layer independent of the API and the data layer | [ADR 0001](adr/0001-maven-multi-module-clean-architecture.md) | P1, P2 | [x] |
| Unit tests for every core component | [testing-strategy §2–3](testing-strategy.md) | P1–P4 | [x] |
| Optional: extra functionality | Offline mode, mutation testing, OpenAPI | P6 | [x] |
| Security aligned with OWASP (extra) | [security.md](security.md), [ADR 0008](adr/0008-httponly-cookie-token-and-csrf.md) | P3–P6 | [x] |

## Frontend

| Requirement | Designed in | Delivered in | Done |
|---|---|---|---|
| Modern framework (React) | Next.js 16 App Router ([ADR 0010](adr/0010-nextjs-app-router.md)) + HeroUI v3 ([ADR 0009](adr/0009-heroui-v3-component-library.md)) + Redux Toolkit ([ADR 0006](adr/0006-react-redux-toolkit.md)) | P5 | [x] |
| Responsive, user-centric design | [ui-ux.md](ui-ux.md) §1–7, §9 | P5 | [x] |
| CRUD operations for the user stories | [architecture §3](architecture.md) | P5 | [x] |
| Clean component structure and efficient state management | [architecture §3.1](architecture.md) | P5 | [x] |
| Nice-to-have: no browser console warnings | Playwright console guard | P5 | [ ] (pages are clean in the production build; the automated guard lands with the Playwright PR) |

## Delivery

| Requirement | Designed in | Delivered in | Done |
|---|---|---|---|
| README with setup steps and technical documentation | [README](../README.md) | P7 | [x] |
| Seeded demo data and demo credentials | [roadmap P6](roadmap.md) | P6 | [x] |
| Dockerfile for running in a container | [roadmap P6](roadmap.md) | P6 | [x] |

## GenAI section

| Requirement | Designed in | Delivered in | Done |
|---|---|---|---|
| Prompt used to generate the task-management API | [task-management-exercise §1](genai/task-management-exercise.md) | P7 | [x] drafted |
| Output code (or a representative sample) | [task-management-exercise §2](genai/task-management-exercise.md) | P7 | [ ] |
| How the AI's suggestions were checked and corrected | [task-management-exercise §3–4](genai/task-management-exercise.md) | P7 | [ ] |
| Handling of edge cases, authentication and validation | [task-management-exercise §3–4](genai/task-management-exercise.md) | P7 | [ ] |

## Presentation (evaluation criteria)

| Criterion | Evidence |
|---|---|
| Clean Architecture | Module diagram, enforcer + ArchUnit, ADR 0001/0002 |
| Application testing / TDD | JaCoCo + PIT reports, `test:` → `feat:` commit history |
| Code quality | Spotless, small use-case classes, value objects |
| Functionality, no console warnings | Live demo, Playwright console guard |
| Presentation (backend + frontend practice) | ProblemDetail errors, [OWASP-aligned security](security.md), caching, RTK Query, [responsive, accessible UI](ui-ux.md) |
| GenAI tools | [prompt-log](genai/prompt-log.md), [workflow checklist](genai/workflow.md), task-management write-up |
