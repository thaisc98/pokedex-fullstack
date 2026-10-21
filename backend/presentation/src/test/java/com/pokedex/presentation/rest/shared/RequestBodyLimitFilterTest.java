package com.pokedex.presentation.rest.shared;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** security.md §4.3: request bodies over 16 KB are refused with 413 {@code /problems/payload-too-large}. */
class RequestBodyLimitFilterTest {

    private static final int LIMIT = RequestBodyLimitFilter.MAX_BODY_BYTES;

    private final RequestBodyLimitFilter filter = new RequestBodyLimitFilter();

    @Test
    void should_be_16_kb() {
        assertThat(LIMIT).isEqualTo(16 * 1024);
    }

    @Test
    void should_pass_a_body_of_exactly_the_limit_through_unchanged() throws Exception {
        MockHttpServletRequest request = request(LIMIT, true);
        AtomicReference<String> seen = new AtomicReference<>();

        MockHttpServletResponse response = run(request, seen);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(seen.get()).hasSize(LIMIT);
    }

    @Test
    void should_refuse_a_declared_length_over_the_limit_without_reading_it() throws Exception {
        MockHttpServletRequest request = request(LIMIT + 1, true);
        AtomicReference<String> seen = new AtomicReference<>();

        MockHttpServletResponse response = run(request, seen);

        assertPayloadTooLarge(response);
        assertThat(seen.get()).as("chain not called").isNull();
    }

    @Test
    void should_refuse_an_undeclared_length_that_turns_out_too_large() throws Exception {
        MockHttpServletRequest request = request(LIMIT + 1, false);
        AtomicReference<String> seen = new AtomicReference<>();

        MockHttpServletResponse response = run(request, seen);

        assertPayloadTooLarge(response);
        assertThat(seen.get()).isNull();
    }

    @Test
    void should_replay_a_small_undeclared_body_to_the_chain() throws Exception {
        MockHttpServletRequest request = request(10, false);
        AtomicReference<String> seen = new AtomicReference<>();

        MockHttpServletResponse response = run(request, seen);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(seen.get()).isEqualTo("x".repeat(10));
    }

    @Test
    void should_ignore_requests_without_a_body() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/collection");
        AtomicReference<String> seen = new AtomicReference<>();

        assertThat(run(request, seen).getStatus()).isEqualTo(200);
        assertThat(seen.get()).isEmpty();
    }

    private MockHttpServletResponse run(MockHttpServletRequest request, AtomicReference<String> seen) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
                    throws IOException {
                seen.set(
                        new String(((HttpServletRequest) req).getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            }
        });
        return response;
    }

    private static MockHttpServletRequest request(int bytes, boolean declareLength) {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/collection/25") {
            @Override
            public int getContentLength() {
                return declareLength ? super.getContentLength() : -1;
            }

            @Override
            public long getContentLengthLong() {
                return declareLength ? super.getContentLengthLong() : -1;
            }
        };
        request.setContentType("application/json");
        request.setContent("x".repeat(bytes).getBytes(StandardCharsets.UTF_8));
        if (!declareLength) {
            request.addHeader("Transfer-Encoding", "chunked");
        }
        return request;
    }

    private static void assertPayloadTooLarge(MockHttpServletResponse response) throws Exception {
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"type\":\"/problems/payload-too-large\"")
                .contains("\"status\":413");
    }
}
