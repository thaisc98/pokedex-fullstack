package com.pokedex.support;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

/**
 * Common root of every Spring Boot integration test ({@code *IT}). Subclasses must not add their own
 * {@code @SpringBootTest}; configuration that differs (WireMock base URL, a profile) is added on top, and every
 * class with the same additions shares one cached application context.
 *
 * <ul>
 *   <li>Full app on a random port, called over real HTTP with RestAssured.
 *   <li><b>PostgreSQL</b>: one Testcontainer for the whole JVM ({@link Postgres#CONTAINER}), same image as
 *       {@code docker-compose.yml} (ADR 0003), exposed through {@code @ServiceConnection} so Boot builds the
 *       {@code DataSource} and Flyway runs the real migrations. Contexts that differ (default vs {@code demo})
 *       reuse the same container; the data is isolated by unique test data (unique emails per test).
 *   <li><b>JWT_SECRET</b>: a fixed 256-bit test value ({@link #TEST_JWT_SECRET}) so the app starts (it refuses to
 *       start without one, security.md §4.2) and so tests can sign their own tokens to probe validation.
 *   <li>Before every test, every cache of every {@link CacheManager} is cleared. That also resets the Bucket4j
 *       rate-limit buckets, which the backend keeps in a Spring cache named {@code rate-limit} (assumption agreed
 *       with the backend), so one test's attempts never throttle another's.
 * </ul>
 *
 * <p>Abstract, so Failsafe does not run it on its own.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "JWT_SECRET=" + AbstractIntegrationIT.TEST_JWT_SECRET)
@Import(AbstractIntegrationIT.Postgres.class)
public abstract class AbstractIntegrationIT {

    /**
     * 64 ASCII characters = 512 bits when used as raw UTF-8 bytes (the HS256 key, security.md §4.2). Test-only
     * value, never used outside tests.
     */
    public static final String TEST_JWT_SECRET = "test-only-jwt-secret-0123456789-abcdefghijklmnopqrstuvwxyz-ABCD";

    public static final String PROBLEM_JSON = "application/problem+json";

    @Value("${local.server.port}")
    protected int port;

    @Autowired
    private ObjectProvider<CacheManager> cacheManagers;

    /** Also callable mid-test, e.g. to simulate a cache entry expiring between two calls. */
    @BeforeEach
    protected void clearAllCaches() {
        cacheManagers
                .orderedStream()
                .forEach(manager -> manager.getCacheNames().stream()
                        .map(manager::getCache)
                        .filter(Objects::nonNull)
                        .forEach(Cache::clear));
    }

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "";
        // Numbers as BigDecimal so decimal assertions (6.9 kg) are exact, never float-rounded by the test itself.
        RestAssured.config = RestAssuredConfig.config()
                .jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    @Autowired
    private ObjectProvider<JdbcTemplate> jdbc;

    /** Password of every user created by {@link #loggedInUser()} and {@link #loggedInAdmin()}. */
    public static final String TEST_USER_PASSWORD = "Test-User-Password-2026";

    /** A fresh {@code USER}, registered and logged in through the API. */
    protected static Browser loggedInUser() {
        Browser browser = new Browser();
        browser.registerAndLogin(uniqueEmail("user"), TEST_USER_PASSWORD);
        return browser;
    }

    /**
     * A fresh {@code ADMIN}, logged in through the API. There is no API to create one (security.md §3.1: admins are
     * seeded), so the row is inserted straight into {@code app_user} (architecture §2.7 columns) with a BCrypt hash
     * computed here, prefixed {@code {bcrypt}} for the {@code DelegatingPasswordEncoder} (security.md §4.1). Cost 4
     * keeps the test fast; BCrypt verification accepts any cost.
     */
    protected Browser loggedInAdmin() {
        String email = uniqueEmail("admin");
        String hash = "{bcrypt}" + new BCryptPasswordEncoder(4).encode(TEST_USER_PASSWORD);
        jdbc.getObject()
                .update(
                        "INSERT INTO app_user (id, email, password_hash, display_name, role, created_at)"
                                + " VALUES (?, ?, ?, ?, ?, ?)",
                        UUID.randomUUID(),
                        email,
                        hash,
                        "Test Admin",
                        "ADMIN",
                        OffsetDateTime.now(ZoneOffset.UTC));
        Browser browser = new Browser();
        Response login = browser.login(email, TEST_USER_PASSWORD);
        assertThat(login.statusCode()).as("admin login").isEqualTo(200);
        assertThat(login.jsonPath().getString("user.role")).isEqualTo("ADMIN");
        return browser;
    }

    /** A unique, valid, lowercase email for this test run. */
    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    /**
     * A minimal browser for the API: a cookie jar that applies every {@code Set-Cookie} (including deletion via
     * {@code Max-Age=0}), and the SPA's CSRF habit: before a write it echoes the {@code XSRF-TOKEN} cookie in the
     * {@code X-XSRF-TOKEN} header (security.md §4.2.1). Cookies are sent explicitly, so RestAssured's own cookie
     * handling plays no part. Every response is checked for {@code JSESSIONID} (STATELESS, security.md §4.2.1).
     */
    public static final class Browser {

        public static final String CSRF_PATH = "/api/v1/auth/csrf";
        public static final String REGISTER_PATH = "/api/v1/auth/register";
        public static final String LOGIN_PATH = "/api/v1/auth/login";
        public static final String LOGOUT_PATH = "/api/v1/auth/logout";
        public static final String ME_PATH = "/api/v1/auth/me";

        private static final JsonMapper JSON = JsonMapper.builder().build();

        private final Map<String, String> cookies = new LinkedHashMap<>();

        public @Nullable String cookie(String name) {
            return cookies.get(name);
        }

        public void setCookie(String name, String value) {
            cookies.put(name, value);
        }

        public void removeCookie(String name) {
            cookies.remove(name);
        }

        /** {@code GET /auth/csrf}, which sets the readable {@code XSRF-TOKEN} cookie. */
        public Response fetchCsrf() {
            return get(CSRF_PATH);
        }

        public Response get(String path) {
            return track(request().when().get(path));
        }

        /** GET with query parameters, URL-encoded by RestAssured (so {@code %}, {@code _} and {@code \} arrive literally). */
        public Response get(String path, Map<String, ?> query) {
            return track(request().queryParams(query).when().get(path));
        }

        /** A write with {@code X-XSRF-TOKEN} = current {@code XSRF-TOKEN} cookie (fetched first if missing). */
        public Response write(String method, String path, @Nullable Object body) {
            if (cookie("XSRF-TOKEN") == null) {
                fetchCsrf();
            }
            return writeWithCsrfHeader(method, path, body, cookie("XSRF-TOKEN"));
        }

        /** A write with an explicit {@code X-XSRF-TOKEN} value ({@code null} = header absent). */
        public Response writeWithCsrfHeader(String method, String path, @Nullable Object body, @Nullable String token) {
            RequestSpecification spec = request();
            if (token != null) {
                spec = spec.header("X-XSRF-TOKEN", token);
            }
            if (body != null) {
                spec = spec.contentType("application/json").body(body instanceof String s ? s : json(body));
            }
            return track(spec.when().request(method, path));
        }

        /** A write with the CSRF header and an explicit {@code Content-Type} (for 413 / 415 cases). */
        public Response writeRaw(String method, String path, String body, String contentType) {
            if (cookie("XSRF-TOKEN") == null) {
                fetchCsrf();
            }
            RequestSpecification spec = request()
                    .header("X-XSRF-TOKEN", cookie("XSRF-TOKEN"))
                    .contentType(contentType)
                    .body(body);
            return track(spec.when().request(method, path));
        }

        public Response register(String email, String password, String displayName) {
            return write(
                    "POST", REGISTER_PATH, Map.of("email", email, "password", password, "displayName", displayName));
        }

        public Response login(String email, String password) {
            return write("POST", LOGIN_PATH, Map.of("email", email, "password", password));
        }

        /** Registers and logs in a fresh {@code USER}; returns the login response. */
        public Response registerAndLogin(String email, String password) {
            Response registered = register(email, password, "Test User");
            assertThat(registered.statusCode()).as("register %s", email).isEqualTo(201);
            Response login = login(email, password);
            assertThat(login.statusCode()).as("login %s", email).isEqualTo(200);
            assertThat(cookie("access_token"))
                    .as("access_token cookie after login")
                    .isNotBlank();
            return login;
        }

        public Response logout() {
            return write("POST", LOGOUT_PATH, null);
        }

        private RequestSpecification request() {
            RequestSpecification spec = given().urlEncodingEnabled(true);
            if (!cookies.isEmpty()) {
                spec = spec.header("Cookie", cookieHeader());
            }
            return spec;
        }

        private String cookieHeader() {
            StringBuilder header = new StringBuilder();
            cookies.forEach((name, value) -> {
                if (!header.isEmpty()) {
                    header.append("; ");
                }
                header.append(name).append('=').append(value);
            });
            return header.toString();
        }

        private Response track(Response response) {
            for (String setCookie : response.getHeaders().getValues("Set-Cookie")) {
                assertThat(setCookie)
                        .as("no server session is ever created (STATELESS)")
                        .doesNotContainIgnoringCase("JSESSIONID");
                SetCookie parsed = SetCookie.parse(setCookie);
                if (parsed.deletes()) {
                    cookies.remove(parsed.name());
                } else {
                    cookies.put(parsed.name(), parsed.value());
                }
            }
            return response;
        }

        static String json(Object body) {
            return JSON.writeValueAsString(body);
        }
    }

    /** One parsed {@code Set-Cookie} header: name, value and attributes (names lower-cased). */
    public record SetCookie(String name, String value, Map<String, String> attributes) {

        public static SetCookie parse(String header) {
            String[] parts = header.split(";");
            String pair = parts[0].strip();
            int eq = pair.indexOf('=');
            String name = eq < 0 ? pair : pair.substring(0, eq).strip();
            String value = eq < 0 ? "" : pair.substring(eq + 1).strip();
            Map<String, String> attributes = new LinkedHashMap<>();
            for (int i = 1; i < parts.length; i++) {
                String attribute = parts[i].strip();
                if (attribute.isEmpty()) {
                    continue;
                }
                int aeq = attribute.indexOf('=');
                if (aeq < 0) {
                    attributes.put(attribute.toLowerCase(Locale.ROOT), "");
                } else {
                    attributes.put(
                            attribute.substring(0, aeq).strip().toLowerCase(Locale.ROOT),
                            attribute.substring(aeq + 1).strip());
                }
            }
            return new SetCookie(name, value, attributes);
        }

        /** The {@code Set-Cookie} headers of a response for one cookie name. */
        public static List<SetCookie> all(Response response, String name) {
            return response.getHeaders().getValues("Set-Cookie").stream()
                    .map(SetCookie::parse)
                    .filter(c -> c.name().equals(name))
                    .toList();
        }

        public boolean deletes() {
            return "0".equals(attributes.get("max-age")) || value.isEmpty();
        }

        public boolean has(String attribute) {
            return attributes.containsKey(attribute.toLowerCase(Locale.ROOT));
        }

        public @Nullable String attribute(String attribute) {
            return attributes.get(attribute.toLowerCase(Locale.ROOT));
        }
    }

    /** One PostgreSQL container for every context in the JVM; Boot connects to it via {@code @ServiceConnection}. */
    @TestConfiguration(proxyBeanMethods = false)
    public static class Postgres {

        /** Same image (tag + digest) as {@code docker-compose.yml}. */
        static final DockerImageName IMAGE = DockerImageName.parse(
                        "postgres:17.11-alpine@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                // the digest form is not recognised as "postgres" by name alone; it is the official image
                .asCompatibleSubstituteFor("postgres");

        static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(IMAGE);

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return CONTAINER;
        }
    }
}
