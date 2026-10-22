package com.pokedex.presentation.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.catalog.port.in.GetPokemonDetailUseCase;
import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.application.catalog.port.in.SearchPokemonNamesUseCase;
import com.pokedex.presentation.rest.catalog.CatalogController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Outside the demo profile the springdoc routes fall under deny-by-default (security.md §3.2): 401. */
@WebMvcTest(CatalogController.class)
@Import(SecurityConfig.class)
class ApiDocsDeniedByDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPokemonUseCase listPokemon;

    @MockitoBean
    private GetPokemonDetailUseCase getPokemonDetail;

    @MockitoBean
    private SearchPokemonNamesUseCase searchPokemonNames;

    @ParameterizedTest
    @ValueSource(
            strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui/index.html", "/swagger-ui.html"})
    void should_deny_api_docs_and_swagger_ui_to_anonymous_callers(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/unauthorized"));
    }
}
