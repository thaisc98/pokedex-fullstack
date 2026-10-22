# syntax=docker/dockerfile:1
# Pokédex backend: multi-stage build → small JRE image, non-root (docs/security.md §4.10).
# Base images pinned by tag AND digest; bump both together (Dependabot's docker ecosystem can automate this).

FROM eclipse-temurin:25-jdk-alpine@sha256:3fd2d245c4e0eba615fe366a71b8bd25f5db7104f53e4026b24bf508b880bd2a AS build
WORKDIR /src
COPY . .
# Tests run in CI (they need Docker for Testcontainers); the image build only packages the app.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests -Djacoco.skip=true -pl bootstrap -am package \
 && cp bootstrap/target/pokedex-bootstrap-*.jar /src/app.jar

FROM eclipse-temurin:25-jre-alpine@sha256:3c0a9084927a221ccd1d007fcaf614465672c0af37aaa834c5184483afe56d61
WORKDIR /app
RUN addgroup -S pokedex && adduser -S -G pokedex -H pokedex
COPY --from=build --chown=pokedex:pokedex /src/app.jar app.jar
USER pokedex
EXPOSE 8080
# A public endpoint that never calls PokeAPI, so the check doesn't depend on the internet.
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=6 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
