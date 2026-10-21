package com.pokedex.presentation.rest.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.catalog.PokemonDetailFixtures;
import com.pokedex.application.catalog.port.in.GetPokemonDetailUseCase;
import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.PokemonRef;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import com.pokedex.presentation.security.SecurityConfig;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** {@code GET /api/v1/pokemon/{idOrName}} slice: idOrName rule (openapi), 404/400/503 ProblemDetails. */
@WebMvcTest(CatalogController.class)
@Import(SecurityConfig.class)
class CatalogDetailControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPokemonUseCase listPokemon;

    @MockitoBean
    private GetPokemonDetailUseCase getPokemonDetail;

    @Test
    void should_return_the_detail_by_id() throws Exception {
        when(getPokemonDetail.get(new PokemonRef.ById(new PokedexId(25)))).thenReturn(PokemonDetailFixtures.pikachu());

        mockMvc.perform(get("/api/v1/pokemon/25"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.name").value("pikachu"))
                .andExpect(jsonPath("$.types[0]").value("electric"))
                .andExpect(jsonPath("$.stats[5].name").value("speed"))
                .andExpect(jsonPath("$.height").value(0.4))
                .andExpect(jsonPath("$.mass").value(6.0))
                .andExpect(jsonPath("$.evolution.root.name").value("pichu"))
                .andExpect(
                        jsonPath("$.evolution.root.evolvesTo[0].trigger.kind").value("level-up"))
                .andExpect(jsonPath("$.evolution.root.evolvesTo[0].trigger.minLevel")
                        .doesNotExist());
    }

    @Test
    void should_lowercase_a_name_before_looking_it_up() throws Exception {
        when(getPokemonDetail.get(any())).thenReturn(PokemonDetailFixtures.pikachu());

        mockMvc.perform(get("/api/v1/pokemon/Pikachu")).andExpect(status().isOk());

        verify(getPokemonDetail).get(new PokemonRef.ByName(new PokemonName("pikachu")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "00", "99999999999"})
    void should_answer_not_found_for_ids_that_cannot_exist_without_calling_the_use_case(String idOrName)
            throws Exception {
        mockMvc.perform(get("/api/v1/pokemon/{idOrName}", idOrName))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/not-found"))
                .andExpect(jsonPath("$.status").value(404));

        verify(getPokemonDetail, never()).get(any());
    }

    @Test
    void should_answer_not_found_when_the_catalog_has_no_such_pokemon() throws Exception {
        when(getPokemonDetail.get(any())).thenThrow(new NotFoundException("pokemon"));

        mockMvc.perform(get("/api/v1/pokemon/missingno"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/problems/not-found"))
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value(not(containsString("missingno"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pika_chu", "pika.chu", "pikachu!", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void should_reject_a_malformed_name_with_a_validation_problem_on_id_or_name(String idOrName) throws Exception {
        mockMvc.perform(get("/api/v1/pokemon/{idOrName}", idOrName))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("idOrName"));

        verify(getPokemonDetail, never()).get(any());
    }

    @Test
    void should_reject_matrix_parameters_instead_of_silently_dropping_them() throws Exception {
        // Spring MVC would bind {idOrName} = "pika" and drop ";drop". StrictHttpFirewall rejects the URL first;
        // its rejection is answered as a validation ProblemDetail, and the use case is never reached.
        mockMvc.perform(get("/api/v1/pokemon/pika;drop"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("path"))
                .andExpect(content().string(not(containsString("drop"))));

        verify(getPokemonDetail, never()).get(any());
    }

    @Test
    void should_reject_a_raw_uri_with_a_semicolon_even_if_it_reaches_the_controller() {
        // Defence in depth: the firewall normally stops this first (see the test above).
        CatalogController controller = new CatalogController(listPokemon, getPokemonDetail);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/pokemon/pika;drop");

        assertThatThrownBy(() -> controller.detail("pika", request))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("idOrName"));
        verify(getPokemonDetail, never()).get(any());
    }

    @Test
    void should_return_503_when_the_catalog_is_unavailable() throws Exception {
        when(getPokemonDetail.get(any()))
                .thenThrow(new UpstreamUnavailableException("PokeAPI is unavailable", new IOException("reset")));

        mockMvc.perform(get("/api/v1/pokemon/25"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("/problems/upstream-unavailable"));
    }
}
