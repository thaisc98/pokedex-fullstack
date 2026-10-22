package com.pokedex.presentation.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.collection.port.in.DeleteCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.GetCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.ListCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.SyncPokemonUseCase;
import com.pokedex.application.collection.port.in.SyncResult;
import com.pokedex.application.collection.port.in.UpdateCollectedPokemonUseCase;
import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.BaseStat;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.StatName;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.collection.UpstreamSnapshot;
import com.pokedex.domain.collection.vo.CollectedPokemonId;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.presentation.rest.collection.CollectionController;
import com.pokedex.presentation.rest.shared.StrictJsonConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Authorization matrix rows for the collection (security.md §3, BR-10): anonymous → 401 everywhere; USER may read,
 * sync and update; only ADMIN may delete (USER → 403 /problems/forbidden from the filter chain, before the use case
 * runs). CSRF on collection writes is in {@code CollectionCsrfTest}.
 */
@WebMvcTest(CollectionController.class)
@Import({SecurityConfig.class, StrictJsonConfig.class})
class CollectionAuthorizationMatrixTest {

    private static final String UPDATE_BODY =
            "{\"customAttributes\":{\"localizedNames\":[],\"tags\":[]},\"version\":0}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SyncPokemonUseCase sync;

    @MockitoBean
    private ListCollectedPokemonUseCase list;

    @MockitoBean
    private GetCollectedPokemonUseCase getOne;

    @MockitoBean
    private UpdateCollectedPokemonUseCase update;

    @MockitoBean
    private DeleteCollectedPokemonUseCase deleteOne;

    @BeforeEach
    void stubUseCases() {
        CollectedPokemon pikachu = pikachu();
        when(sync.sync(any())).thenReturn(new SyncResult(pikachu, true));
        when(list.list(any())).thenReturn(new Page<>(List.of(), PageRequest.ofPage(0), 0));
        when(getOne.get(any())).thenReturn(pikachu);
        when(update.update(any())).thenReturn(pikachu);
    }

    @ParameterizedTest(name = "{0} {1} as {2} -> {3}")
    @CsvSource({
        "GET,    /api/v1/collection,       anonymous, 401",
        "POST,   /api/v1/collection,       anonymous, 401",
        "GET,    /api/v1/collection/25,    anonymous, 401",
        "PUT,    /api/v1/collection/25,    anonymous, 401",
        "DELETE, /api/v1/collection/25,    anonymous, 401",
        "GET,    /api/v1/collection,       USER,      200",
        "POST,   /api/v1/collection,       USER,      201",
        "GET,    /api/v1/collection/25,    USER,      200",
        "PUT,    /api/v1/collection/25,    USER,      200",
        "DELETE, /api/v1/collection/25,    USER,      403",
        "GET,    /api/v1/collection,       ADMIN,     200",
        "POST,   /api/v1/collection,       ADMIN,     201",
        "PUT,    /api/v1/collection/25,    ADMIN,     200",
        "DELETE, /api/v1/collection/25,    ADMIN,     204",
        // deny by default below the collection
        "GET,    /api/v1/collection/25/x,  USER,      403",
        "PATCH,  /api/v1/collection/25,    USER,      403",
        "PUT,    /api/v1/collection,       USER,      403",
        "DELETE, /api/v1/collection,       ADMIN,     403"
    })
    void should_enforce_the_collection_matrix(String method, String path, String caller, int expected)
            throws Exception {
        mockMvc.perform(as(
                        caller, body(request(HttpMethod.valueOf(method), path).with(csrf()), method, path)))
                .andExpect(status().is(expected));
    }

    @Test
    void should_answer_a_user_delete_with_the_forbidden_problem_without_calling_the_use_case() throws Exception {
        mockMvc.perform(as(
                        "USER",
                        request(HttpMethod.DELETE, "/api/v1/collection/25").with(csrf())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/forbidden"));
        verifyNoInteractions(deleteOne);
    }

    @Test
    void should_never_let_a_personal_collection_response_be_cached() throws Exception {
        // security.md §4.7: only the public catalog is cacheable; the collection is per user -> no-store
        mockMvc.perform(as("USER", request(HttpMethod.GET, "/api/v1/collection")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    static MockHttpServletRequestBuilder as(String caller, MockHttpServletRequestBuilder request) {
        return switch (caller) {
            case "USER" -> request.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")));
            case "ADMIN" ->
                request.with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));
            default -> request;
        };
    }

    private static MockHttpServletRequestBuilder body(
            MockHttpServletRequestBuilder request, String method, String path) {
        if ("POST".equals(method)) {
            return request.contentType(MediaType.APPLICATION_JSON).content("{\"pokedexId\":25}");
        }
        if ("PUT".equals(method) && path.matches(".*/\\d+")) {
            return request.contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY);
        }
        return request;
    }

    static CollectedPokemon pikachu() {
        UpstreamSnapshot snapshot = new UpstreamSnapshot(
                new PokedexId(25),
                new PokemonName("pikachu"),
                ImageUrl.PLACEHOLDER,
                Category.UNKNOWN,
                new Mass(60),
                List.of(new Ability("static", false)),
                List.of(new BaseStat(StatName.HP, 35)));
        Instant at = Instant.parse("2026-10-21T10:00:00Z");
        return CollectedPokemon.rehydrate(CollectedPokemonId.random(), snapshot, CustomAttributes.EMPTY, 0, at, at);
    }
}
