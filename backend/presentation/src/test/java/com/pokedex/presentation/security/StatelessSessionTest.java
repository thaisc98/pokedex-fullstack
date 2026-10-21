package com.pokedex.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.presentation.rest.catalog.CatalogController;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * STATELESS (security.md §4.2): nothing in the filter chain or the controllers may create an HTTP session.
 * A first-in-chain filter wraps the request and counts every call that would create one. (MockMvc's own test
 * infrastructure may attach a mock session outside the chain, so {@code getSession(false)} on the mock
 * request is not a reliable signal; the real-HTTP {@code JSESSIONID} check lives in the acceptance tests.)
 */
@WebMvcTest(CatalogController.class)
@Import({SecurityConfig.class, StatelessSessionTest.SessionCreationCounter.class})
class StatelessSessionTest {

    static final AtomicInteger SESSIONS_CREATED = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPokemonUseCase listPokemon;

    @BeforeEach
    void reset() {
        SESSIONS_CREATED.set(0);
        when(listPokemon.list(any())).thenReturn(new Page<>(List.of(), PageRequest.ofPage(0), 0));
    }

    @Test
    void should_not_create_a_session_for_a_public_read() throws Exception {
        mockMvc.perform(get("/api/v1/pokemon"));

        assertThat(SESSIONS_CREATED).hasValue(0);
    }

    @Test
    void should_not_create_a_session_when_access_is_denied() throws Exception {
        mockMvc.perform(get("/api/v1/collection"));
        mockMvc.perform(post("/api/v1/pokemon"));
        mockMvc.perform(post("/api/v1/pokemon").with(csrf()));

        assertThat(SESSIONS_CREATED).hasValue(0);
    }

    @TestConfiguration
    static class SessionCreationCounter {

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        Filter sessionCreationCounter() {
            return (request, response, chain) -> chain.doFilter(
                    new HttpServletRequestWrapper((HttpServletRequest) request) {
                        @Override
                        public HttpSession getSession(boolean create) {
                            if (create && super.getSession(false) == null) {
                                SESSIONS_CREATED.incrementAndGet();
                            }
                            return super.getSession(create);
                        }

                        @Override
                        public HttpSession getSession() {
                            return getSession(true);
                        }
                    },
                    response);
        }
    }
}
