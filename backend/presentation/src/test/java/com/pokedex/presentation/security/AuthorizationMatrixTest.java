package com.pokedex.presentation.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.presentation.rest.catalog.CatalogController;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Authorization matrix (security.md §3, BR-10): every endpoint × caller. Deny by default: anything without a
 * row here is refused. Every new endpoint must add its rows.
 */
@WebMvcTest(CatalogController.class)
@Import(SecurityConfig.class)
class AuthorizationMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPokemonUseCase listPokemon;

    @BeforeEach
    void emptyCatalog() {
        when(listPokemon.list(any())).thenReturn(new Page<>(List.of(), PageRequest.ofPage(0), 0));
    }

    @ParameterizedTest(name = "anonymous {0} {1} -> {2}")
    @CsvSource({
        // US01: the catalog list is public
        "GET,    /api/v1/pokemon,        200",
        // deny by default: everything not listed
        "GET,    /api/v1/pokemon/25,     401",
        "GET,    /api/v1/collection,     401",
        "GET,    /api/v1/anything-else,  401",
        "GET,    /,                      401",
        "GET,    /sample,                401",
        "POST,   /api/v1/pokemon,        401",
        "PUT,    /api/v1/pokemon,        401",
        "DELETE, /api/v1/pokemon,        401"
    })
    void should_enforce_the_matrix_for_anonymous_callers(String method, String path, int expected) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path).with(csrf())).andExpect(status().is(expected));
    }

    @Test
    void should_answer_401_as_a_problem_detail_without_a_basic_auth_challenge() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v1/anything-else"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(header().doesNotExist("WWW-Authenticate"))
                .andExpect(jsonPath("$.type").value("/problems/unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void should_reject_a_write_without_a_csrf_token_with_403() throws Exception {
        mockMvc.perform(request(HttpMethod.POST, "/api/v1/pokemon"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("/problems/csrf-token-invalid"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void should_reject_a_write_with_an_invalid_csrf_token_with_403() throws Exception {
        mockMvc.perform(request(HttpMethod.POST, "/api/v1/pokemon").with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/csrf-token-invalid"));
    }

    @Test
    void should_never_set_a_session_cookie_for_the_public_catalog() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v1/pokemon"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test
    void should_send_the_security_headers() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v1/pokemon"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
