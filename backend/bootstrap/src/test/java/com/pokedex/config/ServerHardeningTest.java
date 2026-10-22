package com.pokedex.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokedex.presentation.security.ratelimit.RateLimitedException;
import com.pokedex.presentation.security.ratelimit.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server settings from {@code application.yml} on a real Tomcat (no database): the client IP that the rate limits
 * key on (security.md §4.1) and BREACH protection for the CSRF token (§4.2.1). The probe uses the same
 * {@link RateLimiter} and the same key ({@code request.getRemoteAddr()}) as the registration endpoint.
 */
class ServerHardeningTest {

    /**
     * The probe's own minimal Boot app (web only, no database). Marked {@code @TestComponent}, so the full application's
     * component scan in the ITs skips it and the authorization matrix never sees the probe endpoints.
     */
    @SpringBootConfiguration(proxyBeanMethods = false)
    @TestComponent
    @Import(ProbeController.class)
    @ImportAutoConfiguration({
        TomcatServletWebServerAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        WebMvcAutoConfiguration.class
    })
    static class Probe {}

    @TestComponent
    @RestController
    static class ProbeController {

        private final RateLimiter registrations =
                new RateLimiter(new ConcurrentMapCache("rate-limit"), "register", 3, Duration.ofMinutes(1));

        @GetMapping("/probe/register")
        ResponseEntity<String> register(HttpServletRequest request) {
            try {
                registrations.consume(String.valueOf(request.getRemoteAddr()));
                return ResponseEntity.ok(request.getRemoteAddr());
            } catch (RateLimitedException e) {
                return ResponseEntity.status(429).body(request.getRemoteAddr());
            }
        }

        /** Large enough (8 KB) that Tomcat would compress it if compression were on. */
        @GetMapping("/probe/large")
        String large() {
            return "x".repeat(8 * 1024);
        }
    }

    @Nested
    @SpringBootTest(classes = Probe.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    class ThroughTheTrustedProxy {

        @LocalServerPort
        int port;

        @Test
        void should_give_each_forwarded_client_its_own_registration_bucket() throws Exception {
            for (int i = 0; i < 3; i++) {
                assertThat(get(port, "/probe/register", "203.0.113.10").body()).isEqualTo("203.0.113.10");
            }

            assertThat(get(port, "/probe/register", "203.0.113.10").statusCode())
                    .isEqualTo(429);
            assertThat(get(port, "/probe/register", "203.0.113.20").statusCode())
                    .isEqualTo(200);
        }

        @Test
        void should_never_compress_responses() throws Exception {
            HttpResponse<String> response = get(port, "/probe/large", null);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Encoding")).isEmpty();
        }
    }

    @Nested
    @SpringBootTest(
            classes = Probe.class,
            webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
            properties = "TRUSTED_PROXIES=10\\.9\\.9\\.9")
    class FromAnUntrustedAddress {

        @LocalServerPort
        int port;

        @Test
        void should_ignore_a_spoofed_x_forwarded_for() throws Exception {
            HttpResponse<String> response = get(port, "/probe/register", "203.0.113.30");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                    .as("the real loopback peer, not the spoofed header")
                    .isIn("127.0.0.1", "0:0:0:0:0:0:0:1");
        }
    }

    private static HttpResponse<String> get(int port, String path, @Nullable String forwardedFor)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept-Encoding", "gzip");
        if (forwardedFor != null) {
            request.header("X-Forwarded-For", forwardedFor);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
