package com.pokedex.presentation.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.catalog.port.in.GetPokemonDetailUseCase;
import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.application.catalog.port.in.SearchPokemonNamesUseCase;
import com.pokedex.application.identity.port.in.AuthenticateUserUseCase;
import com.pokedex.application.identity.port.in.GetCurrentUserUseCase;
import com.pokedex.application.identity.port.in.RegisterUserUseCase;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.presentation.rest.catalog.CatalogController;
import com.pokedex.presentation.rest.identity.AuthController;
import com.pokedex.presentation.rest.shared.StrictJsonConfig;
import com.pokedex.presentation.security.ratelimit.AuthRateLimits;
import com.pokedex.presentation.security.ratelimit.RateLimiter;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * security.md §4.7: every API response, success or error, carries the API security headers. HSTS only over HTTPS.
 */
@WebMvcTest(
        controllers = {CatalogController.class, AuthController.class},
        properties = "pokedex.test.context=security-headers")
@Import({SecurityConfig.class, StrictJsonConfig.class, SecurityHeadersTest.Beans.class})
class SecurityHeadersTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Beans {
        @Bean
        AuthRateLimits authRateLimits() {
            ConcurrentMapCache cache = new ConcurrentMapCache("rate-limit");
            return new AuthRateLimits(
                    new RateLimiter(cache, "login", 5, Duration.ofMinutes(1)),
                    new RateLimiter(cache, "register", 3, Duration.ofMinutes(1)));
        }

        @Bean
        AccessTokenCookies accessTokenCookies() {
            return new AccessTokenCookies(Duration.ofMinutes(60));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPokemonUseCase listPokemon;

    @MockitoBean
    private GetPokemonDetailUseCase getPokemonDetail;

    @MockitoBean
    private SearchPokemonNamesUseCase searchPokemonNames;

    @MockitoBean
    private RegisterUserUseCase registerUser;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUser;

    @MockitoBean
    private GetCurrentUserUseCase getCurrentUser;

    @BeforeEach
    void stubs() {
        when(listPokemon.list(any())).thenReturn(new Page<>(List.of(), PageRequest.ofPage(0), 0));
        when(getPokemonDetail.get(any())).thenThrow(new NotFoundException("pokemon"));
    }

    @ParameterizedTest(name = "GET {0} -> {1} with the API security headers")
    @CsvSource({
        "/api/v1/pokemon,          200", // catalog
        "/api/v1/auth/csrf,        200", // auth
        "/api/v1/auth/me,          401", // error from the security chain
        "/api/v1/pokemon/missingno, 404" // error from the controller advice
    })
    void should_send_the_api_security_headers_on_every_response(String path, int expected) throws Exception {
        assertApiHeaders(mockMvc.perform(get(path)).andExpect(status().is(expected)));
    }

    @Test
    void should_send_hsts_only_over_https() throws Exception {
        mockMvc.perform(get("/api/v1/pokemon").secure(true))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"));
        mockMvc.perform(get("/api/v1/pokemon")).andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    private static void assertApiHeaders(ResultActions response) throws Exception {
        response.andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }
}
