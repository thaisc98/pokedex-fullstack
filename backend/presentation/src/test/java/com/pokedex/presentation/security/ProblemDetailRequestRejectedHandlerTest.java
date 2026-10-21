package com.pokedex.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.firewall.RequestRejectedException;

/**
 * URLs refused by StrictHttpFirewall (e.g. {@code ;} matrix content, encoded slashes) get a 400 validation
 * ProblemDetail instead of a bare error page. Nothing from the URL or the rejection reason is echoed.
 */
class ProblemDetailRequestRejectedHandlerTest {

    @Test
    void should_answer_400_validation_problem_without_echoing_the_url() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/pokemon/pika;drop");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ProblemDetailSecurityHandler()
                .handle(
                        request,
                        response,
                        new RequestRejectedException("The request was rejected because the URL"
                                + " contained a potentially malicious String \";\""));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"type\":\"/problems/validation-error\"")
                .contains("\"field\":\"path\"")
                .doesNotContain("drop")
                .doesNotContain("malicious");
    }
}
