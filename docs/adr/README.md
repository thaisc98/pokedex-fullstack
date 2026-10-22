# Architecture Decision Records

Short records of the important decisions: the context, the decision, its consequences, and the alternatives we rejected.

| # | Title | Status |
|---|---|---|
| [0001](0001-maven-multi-module-clean-architecture.md) | Maven multi-module build that enforces Clean Architecture | Accepted |
| [0002](0002-catalog-vs-collection-split.md) | Read-only Catalog kept separate from the local Collection | Accepted |
| [0003](0003-postgresql-flyway-testcontainers.md) | PostgreSQL + Flyway, Testcontainers in tests | Accepted |
| [0004](0004-caffeine-cache-decorator.md) | Caffeine cache in a decorator around the PokeAPI adapter | Accepted (sync behaviour refined by 0013) |
| [0005](0005-jwt-oauth2-resource-server.md) | Stateless JWT via Spring OAuth2 Resource Server | Accepted (token delivery changed by 0008) |
| [0006](0006-react-redux-toolkit.md) | React + TypeScript + Redux Toolkit (RTK Query) | Accepted (framework replaced by 0010; token storage changed by 0008) |
| [0007](0007-java-25-spring-boot-4.md) | Java 25 + Spring Boot 4.1.1 | Accepted |
| [0008](0008-httponly-cookie-token-and-csrf.md) | JWT in an httpOnly cookie, with CSRF protection | Accepted |
| [0009](0009-heroui-v3-component-library.md) | HeroUI v3 as the UI component library | Accepted |
| [0010](0010-nextjs-app-router.md) | Next.js 16 (App Router) instead of Vite + React Router | Accepted |
| [0011](0011-nginx-edge-proxy-client-ip.md) | nginx edge proxy decides the client IP | Accepted |
| [0012](0012-signed-in-hint-cookie.md) | `pokedex_signed_in` hint cookie for the optimistic route guard | Accepted |
| [0013](0013-collection-sync-live-and-cache-refresh.md) | Collection sync reads PokeAPI live and refreshes the catalog cache | Accepted |
| [0014](0014-offline-demo-compose-override.md) | Offline demo through a Compose override with a WireMock PokeAPI | Accepted |

New ADRs copy this structure: **Status · Context · Decision · Consequences · Alternatives considered**.
