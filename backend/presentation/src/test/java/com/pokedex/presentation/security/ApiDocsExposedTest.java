package com.pokedex.presentation.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.catalog.port.in.GetPokemonDetailUseCase;
import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.presentation.rest.catalog.CatalogController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * With {@code pokedex.security.api-docs-public=true} (set only by {@code application-demo.yml}) anonymous GETs reach
 * the springdoc routes. The slice has no springdoc endpoints, so "let through" shows up as 404 instead of 401.
 * Writes and every other route stay denied.
 */
@WebMvcTest(controllers = CatalogController.class, properties = "pokedex.security.api-docs-public=true")
@Import(SecurityConfig.class)
class ApiDocsExposedTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPokemonUseCase listPokemon;

    @MockitoBean
    private GetPokemonDetailUseCase getPokemonDetail;

    @ParameterizedTest
    @ValueSource(
            strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui/index.html", "/swagger-ui.html"})
    void should_let_anonymous_reads_through_to_the_docs(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/swagger-ui/index.html"})
    void should_still_require_a_csrf_token_for_writes(String path) throws Exception {
        mockMvc.perform(post(path))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/csrf-token-invalid"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs-evil", "/swagger-uix/index.html", "/api/v1/collection", "/"})
    void should_keep_denying_everything_else(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/unauthorized"));
    }
}
