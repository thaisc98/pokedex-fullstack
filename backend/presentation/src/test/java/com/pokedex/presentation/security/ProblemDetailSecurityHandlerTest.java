package com.pokedex.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

/** Filter-chain security failures as fixed problem+json bodies (security.md §4.6). */
class ProblemDetailSecurityHandlerTest {

    private final ProblemDetailSecurityHandler handler = new ProblemDetailSecurityHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void should_answer_403_forbidden_problem_when_an_authenticated_caller_lacks_the_role() throws Exception {
        handler.handle(request, response, new AccessDeniedException("ROLE_ADMIN required for /secret"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"type\":\"/problems/forbidden\"")
                .doesNotContain("ROLE_ADMIN")
                .doesNotContain("/secret");
    }

    @Test
    void should_answer_403_csrf_problem_for_a_missing_token() throws Exception {
        handler.handle(request, response, new MissingCsrfTokenException("abc"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"type\":\"/problems/csrf-token-invalid\"");
    }

    @Test
    void should_answer_401_problem_without_echoing_the_cause() throws Exception {
        handler.commence(request, response, new InsufficientAuthenticationException("token for alice expired"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isNull();
        assertThat(response.getContentAsString())
                .contains("\"type\":\"/problems/unauthorized\"")
                .doesNotContain("alice");
    }
}
