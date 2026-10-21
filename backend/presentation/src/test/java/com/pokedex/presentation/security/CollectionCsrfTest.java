package com.pokedex.presentation.security;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.collection.port.in.DeleteCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.GetCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.ListCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.SyncPokemonUseCase;
import com.pokedex.application.collection.port.in.UpdateCollectedPokemonUseCase;
import com.pokedex.presentation.rest.collection.CollectionController;
import com.pokedex.presentation.rest.shared.StrictJsonConfig;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * security.md §4.2.1: collection writes need {@code X-XSRF-TOKEN} even with a valid access token (the cookie is sent
 * by the browser on its own). Its own application context on purpose: spring-security-test's {@code csrf()} swaps
 * the shared filter's repository, after which a request without a token would no longer be refused. No test here
 * uses {@code csrf()}, and none uses {@code jwt()}: that post-processor calls {@code CsrfFilter.skipRequest}
 * (header bearer tokens are CSRF-exempt), so it can never show that CSRF is enforced.
 */
@WebMvcTest(controllers = CollectionController.class, properties = "pokedex.test.context=collection-csrf")
@Import({SecurityConfig.class, StrictJsonConfig.class})
class CollectionCsrfTest {

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

    private static Authentication signedIn(String caller) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(caller)
                .build();
        return "ADMIN".equals(caller)
                ? new JwtAuthenticationToken(
                        jwt, List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")))
                : new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @ParameterizedTest(name = "{0} {1} as {2} without CSRF -> 403")
    @CsvSource({
        "POST,   /api/v1/collection,    USER",
        "PUT,    /api/v1/collection/25, USER",
        "DELETE, /api/v1/collection/25, ADMIN"
    })
    void should_require_csrf_on_collection_writes_even_when_authenticated(String method, String path, String caller)
            throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .with(authentication(signedIn(caller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/csrf-token-invalid"));
        verifyNoInteractions(sync, update, deleteOne);
    }
}
