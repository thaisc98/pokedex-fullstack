package com.pokedex.presentation.rest.catalog;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.catalog.PokemonDetailFixtures;
import com.pokedex.application.catalog.port.in.GetPokemonDetailUseCase;
import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.application.catalog.port.in.SearchPokemonNamesUseCase;
import com.pokedex.application.identity.port.in.AuthenticateUserUseCase;
import com.pokedex.application.identity.port.in.GetCurrentUserUseCase;
import com.pokedex.application.identity.port.in.RegisterUserUseCase;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonRef;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import com.pokedex.presentation.rest.identity.AuthController;
import com.pokedex.presentation.rest.shared.StrictJsonConfig;
import com.pokedex.presentation.security.AccessTokenCookies;
import com.pokedex.presentation.security.SecurityConfig;
import com.pokedex.presentation.security.ratelimit.AuthRateLimits;
import com.pokedex.presentation.security.ratelimit.RateLimiter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * security.md §4.7 / ADR 0004: only successful public catalog responses may be cached by browsers and shared
 * caches ({@code public, max-age=300, stale-while-revalidate=86400}), with an ETag for 304 revalidation. Every
 * other response, catalog errors included, keeps {@code no-store}.
 */
@WebMvcTest(
        controllers = {CatalogController.class, AuthController.class},
        properties = "pokedex.test.context=catalog-http-cache")
@Import({SecurityConfig.class, StrictJsonConfig.class, CatalogHttpCacheConfig.class, CatalogHttpCacheTest.Beans.class})
class CatalogHttpCacheTest {

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
        when(getPokemonDetail.get(new PokemonRef.ById(new PokedexId(25)))).thenReturn(PokemonDetailFixtures.pikachu());
        when(getPokemonDetail.get(new PokemonRef.ById(new PokedexId(99999))))
                .thenThrow(new NotFoundException("pokemon"));
        when(searchPokemonNames.search(any(), any())).thenReturn(List.of());
    }

    @ParameterizedTest(name = "GET {0} -> 200, public cache")
    @ValueSource(
            strings = {
                "/api/v1/pokemon",
                "/api/v1/pokemon?page=1&size=5",
                "/api/v1/pokemon/25",
                "/api/v1/pokemon/search?q=pika"
            })
    void should_let_successful_catalog_responses_be_cached_publicly(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", publicCatalogCache()))
                .andExpect(header().doesNotExist("Pragma"))
                .andExpect(header().exists("ETag"));
    }

    @ParameterizedTest(name = "GET {0} -> {1}, no-store")
    @CsvSource({
        "/api/v1/pokemon/99999,          404", // catalog: not found
        "/api/v1/pokemon?size=0,         400", // catalog: validation
        "/api/v1/pokemon/search?q=a;b,   400", // catalog search: validation
        "/api/v1/pokemon/pika;drop,      400", // rejected by the firewall
        "/api/v1/auth/csrf,              200", // auth
        "/api/v1/auth/me,                401", // auth, from the security chain
        "/api/v1/does-not-exist,         401" // deny by default
    })
    void should_keep_every_other_response_out_of_caches(String path, int expected) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is(expected))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().doesNotExist("ETag"));
    }

    @Test
    void should_keep_a_catalog_503_out_of_caches() throws Exception {
        when(listPokemon.list(any()))
                .thenThrow(new UpstreamUnavailableException("PokeAPI is unavailable", new IOException("reset")));

        mockMvc.perform(get("/api/v1/pokemon"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().doesNotExist("ETag"));
    }

    @Test
    void should_answer_304_when_the_etag_still_matches() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/v1/pokemon/25")).andReturn();
        String etag = first.getResponse().getHeader("ETag");

        mockMvc.perform(get("/api/v1/pokemon/25").header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", etag))
                .andExpect(header().string("Cache-Control", publicCatalogCache()));
    }

    private static Matcher<String> publicCatalogCache() {
        return allOf(
                containsString("public"),
                containsString("max-age=300"),
                containsString("stale-while-revalidate=86400"),
                not(containsString("no-store")),
                not(containsString("private")));
    }
}
