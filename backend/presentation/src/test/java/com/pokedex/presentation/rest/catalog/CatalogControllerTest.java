package com.pokedex.presentation.rest.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
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
import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
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

/** {@code GET /api/v1/pokemon} slice: status codes, BR-01 validation → 400, ProblemDetail format, 503. */
@WebMvcTest(CatalogController.class)
@Import(SecurityConfig.class)
class CatalogControllerTest {

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
    void should_return_the_page_as_the_contract_envelope() throws Exception {
        when(listPokemon.list(new PageRequest(0, 3)))
                .thenReturn(new Page<>(List.of(CatalogDtoMapperTest.pikachu()), new PageRequest(0, 3), 1351));

        mockMvc.perform(get("/api/v1/pokemon").param("page", "0").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(1351))
                .andExpect(jsonPath("$.totalPages").value(451))
                .andExpect(jsonPath("$.content[0].id").value(25))
                .andExpect(jsonPath("$.content[0].name").value("pikachu"))
                .andExpect(jsonPath("$.content[0].sprite").value(CatalogDtoMapperTest.SPRITE))
                .andExpect(jsonPath("$.content[0].category").value("Mouse Pokémon"))
                .andExpect(jsonPath("$.content[0].mass").value(6.0))
                .andExpect(jsonPath("$.content[0].abilities[1].name").value("lightning-rod"))
                .andExpect(jsonPath("$.content[0].abilities[1].hidden").value(true));
    }

    @Test
    void should_default_to_page_0_and_size_20() throws Exception {
        when(listPokemon.list(PageRequest.ofPage(0))).thenReturn(new Page<>(List.of(), PageRequest.ofPage(0), 0));

        mockMvc.perform(get("/api/v1/pokemon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content").isEmpty());

        verify(listPokemon).list(new PageRequest(0, 20));
    }

    @ParameterizedTest(name = "page={0}, size={1} -> 400 on {2}")
    @CsvSource({
        "0, 0, size",
        "0, 101, size",
        "-1, 20, page",
        "abc, 20, page",
        "0, abc, size",
        "99999999999, 20, page",
        "0, 1.5, size"
    })
    void should_reject_invalid_paging_with_a_validation_problem(String page, String size, String field)
            throws Exception {
        mockMvc.perform(get("/api/v1/pokemon").param("page", page).param("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").value(notNullValue()))
                .andExpect(content().string(not(containsString("Exception"))));

        verify(listPokemon, never()).list(any());
    }

    @Test
    void should_not_echo_the_rejected_parameter_value() throws Exception {
        mockMvc.perform(get("/api/v1/pokemon").param("page", "<script>"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("<script>"))));
    }

    @Test
    void should_return_503_problem_without_internal_details_when_upstream_is_unavailable() throws Exception {
        when(listPokemon.list(any()))
                .thenThrow(new UpstreamUnavailableException(
                        "PokeAPI is unavailable", new IOException("connect to pokeapi.internal:443 refused")));

        mockMvc.perform(get("/api/v1/pokemon"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/upstream-unavailable"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").value("Upstream service unavailable"))
                .andExpect(jsonPath("$.correlationId").value(notNullValue()))
                .andExpect(content().string(not(containsString("pokeapi.internal"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("\"content\""))));
    }

    @Test
    void should_return_a_generic_500_problem_for_an_unexpected_error() throws Exception {
        when(listPokemon.list(any())).thenThrow(new IllegalStateException("secret internal detail"));

        mockMvc.perform(get("/api/v1/pokemon"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/internal-error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.correlationId").value(notNullValue()))
                .andExpect(content().string(not(containsString("secret internal detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void should_report_every_field_name_in_errors() throws Exception {
        mockMvc.perform(get("/api/v1/pokemon").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("size")));
    }

    @Test
    void should_return_the_summary_list_unchanged_for_a_page_past_the_end() throws Exception {
        when(listPokemon.list(new PageRequest(100_000, 20)))
                .thenReturn(new Page<PokemonSummary>(List.of(), new PageRequest(100_000, 20), 1351));

        mockMvc.perform(get("/api/v1/pokemon").param("page", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalPages").value(68));
    }
}
