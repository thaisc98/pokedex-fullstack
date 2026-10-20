# GenAI Exercise — Task-Management REST API

> The separate "Generative AI tools" section of the exercise. Generate a RESTful API for a simple task-management system with a GenAI tool, then show the prompt, the output, and how the output was checked and corrected.

**Requirements given:**
- CRUD for tasks
- Each task has a `title`, `description`, `status` and `due_date`
- Tasks belong to a user (assume a basic `User` model exists)

**Status:** 🟡 Prompt drafted. Output and evaluation still to do (Phase 7).

---

## 1. The prompt

The prompt has five parts: **role → context → requirements → constraints → output format**.

```text
ROLE
You are a senior Java backend engineer who practises Clean Architecture and TDD.

CONTEXT
- Java 25 (no preview features), Spring Boot 4.1.1 (Spring Framework 7, Jackson 3), Maven.
- PostgreSQL with Flyway migrations; Testcontainers for integration tests.
- A User model already exists (id: UUID, email). Authentication is a JWT
  resource server; the current user's id is available from the JWT `sub` claim.

REQUIREMENTS
- CRUD REST API for tasks under /api/v1/tasks.
- Task fields: id (UUID), title, description, status, dueDate, ownerId,
  createdAt, updatedAt, version.
- status is an enum: TODO, IN_PROGRESS, DONE.
- A user can only see and modify their OWN tasks.
- List endpoint is paginated (page, size ≤ 100) and filterable by status.

CONSTRAINTS
- Layers: domain (no framework imports) → application (use cases + ports) →
  infrastructure (JPA adapter) / presentation (controllers + DTOs).
- Validation: title 1–120 chars, not blank; description ≤ 2000;
  dueDate must not be in the past on create; invalid status → 400.
- Errors as RFC 9457 ProblemDetail. 404 for missing OR not-owned tasks.
- Optimistic locking with @Version; stale update → 409.
- Use @MockitoBean (not @MockBean), spring-boot-starter-webmvc, RestClient
  where needed, Jackson 3. No Lombok.
- Write the tests FIRST for the domain and use cases, then the implementation.

OUTPUT FORMAT
1. File tree.
2. Flyway migration.
3. Domain + application code with unit tests.
4. Adapters + controller with @WebMvcTest and one Testcontainers IT.
Keep the explanation short; list any assumptions at the end.
```

## 2. Output (representative sample)

> To do: paste the key generated files here: the domain `Task` record, the `UpdateTaskService`, the controller and one test.

## 3. Critical evaluation

> To do: fill this in from the real output. These are the problems to look for (typical in AI-generated CRUD code):

| # | Problem to look for | Why it matters | Fix |
|---|---|---|---|
| 1 | `status` as a free-text `String` | Invalid states get in | Enum, plus invalid values mapped to 400 |
| 2 | `dueDate` accepted in the past, or timezone ignored | Wrong data | Check against an injected `Clock`; store `LocalDate` or `Instant` consistently |
| 3 | **IDOR:** `findById(id)` without checking the owner | User A can read or edit User B's tasks | Every query filtered by owner; return 404, not 403, so others can't tell the task exists |
| 4 | Entity returned straight from the controller | Layer leak; internal fields exposed | Response DTO + mapper |
| 5 | No pagination on the list endpoint | Unbounded queries | `PageRequest` with a size cap |
| 6 | No `@Version` | Lost updates | Optimistic locking → 409 |
| 7 | `@MockBean`, `javax.*`, `spring-boot-starter-web` | Outdated APIs for Boot 4 | Replace with current equivalents |
| 8 | Tests that only check mock calls | False confidence | Assert on returned results; use fakes |
| 9 | Business rules in the controller | Breaks Clean Architecture | Move them into the domain / use case |

## 4. Validation, edge cases and auth: how each was handled

> To do: for each item above, record whether the AI got it right, what was changed, and the test that now covers it. Link the matching entry in [prompt-log.md](prompt-log.md).
