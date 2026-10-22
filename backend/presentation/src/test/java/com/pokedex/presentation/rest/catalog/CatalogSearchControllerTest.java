package com.pokedex.presentation.rest.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.catalog.port.in.GetPokemonDetailUseCase;
import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.application.catalog.port.in.SearchPokemonNamesUseCase;
import com.pokedex.domain.catalog.PokemonNameRef;
import com.pokedex.domain.catalog.vo.NameQuery;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.SearchLimit;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import com.pokedex.presentation.security.SecurityConfig;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** {@code GET /api/v1/pokemon/search?q=&limit=}: normalization, validation → 400, 503, and route precedence. */
@WebMvcTest(CatalogController.class)
@Import(SecurityConfig.class)
class CatalogSearchControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPokemonUseCase listPokemon;

    @MockitoBean
    private GetPokemonDetailUseCase getPokemonDetail;

    @MockitoBean
    private SearchPokemonNamesUseCase searchPokemonNames;

    @Test
    void should_return_the_results_as_id_and_name() throws Exception {
        when(searchPokemonNames.search(new NameQuery("pika"), SearchLimit.DEFAULT))
                .thenReturn(List.of(ref(25, "pikachu"), ref(10080, "pikachu-rock-star")));

        mockMvc.perform(get("/api/v1/pokemon/search").param("q", "  Pika "))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.results[0].id").value(25))
                .andExpect(jsonPath("$.results[0].name").value("pikachu"))
                .andExpect(jsonPath("$.results[1].id").value(10080))
                .andExpect(jsonPath("$.results.length()").value(2));
    }

    @Test
    void should_pass_the_limit_and_turn_spaces_into_hyphens() throws Exception {
        when(searchPokemonNames.search(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pokemon/search").param("q", "Mr Mime").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isEmpty());

        verify(searchPokemonNames).search(new NameQuery("mr-mime"), new SearchLimit(3));
    }

    @Test
    void should_never_treat_search_as_a_pokemon_name() throws Exception {
        when(searchPokemonNames.search(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pokemon/search").param("q", "pika")).andExpect(status().isOk());

        verify(getPokemonDetail, never()).get(any());
    }

    @ParameterizedTest(name = "q={0}, limit={1} -> 400 on {2}")
    @CsvSource(
            value = {
                "pika;drop, NULL, q",
                "'', NULL, q",
                "'   ', NULL, q",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa, NULL, q",
                "NULL, NULL, q",
                "pika, 0, limit",
                "pika, 21, limit",
                "pika, abc, limit"
            },
            nullValues = "NULL")
    void should_reject_an_invalid_q_or_limit_with_a_validation_problem(String q, String limit, String field)
            throws Exception {
        var request = get("/api/v1/pokemon/search");
        if (q != null) {
            request = request.param("q", q);
        }
        if (limit != null) {
            request = request.param("limit", limit);
        }

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value(field));

        verify(searchPokemonNames, never()).search(any(), any());
    }

    @Test
    void should_return_503_when_the_name_index_cannot_be_loaded() throws Exception {
        when(searchPokemonNames.search(any(), any()))
                .thenThrow(new UpstreamUnavailableException("PokeAPI is unavailable", new IOException("reset")));

        mockMvc.perform(get("/api/v1/pokemon/search").param("q", "pika"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("/problems/upstream-unavailable"));
    }

    private static PokemonNameRef ref(int id, String name) {
        return new PokemonNameRef(new PokedexId(id), new PokemonName(name));
    }
}
