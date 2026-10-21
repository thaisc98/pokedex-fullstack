package com.pokedex.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

/**
 * security.md §4.5: PokeAPI responses are capped (default 1 MB). The cap is enforced up front from {@code
 * Content-Length} and again on the bytes actually read, so a missing or lying header cannot get past it and
 * an oversized body is never buffered.
 */
class ResponseSizeLimitInterceptorTest {

    private static final int LIMIT = 16;

    private final ResponseSizeLimitInterceptor interceptor = new ResponseSizeLimitInterceptor(LIMIT);

    @Test
    void should_reject_up_front_when_content_length_exceeds_the_limit_and_close_the_response() {
        TrackedResponse response = new TrackedResponse(bytes(LIMIT + 1));
        response.getHeaders().setContentLength(LIMIT + 1);

        assertThatThrownBy(() -> intercept(response)).isInstanceOf(ResponseTooLargeException.class);
        assertThat(response.closed).isTrue();
    }

    @Test
    void should_pass_a_body_of_exactly_the_limit_with_a_matching_content_length() throws IOException {
        TrackedResponse response = new TrackedResponse(bytes(LIMIT));
        response.getHeaders().setContentLength(LIMIT);

        assertThat(intercept(response).getBody().readAllBytes()).hasSize(LIMIT);
    }

    @Test
    void should_pass_a_body_of_exactly_the_limit_without_content_length() throws IOException {
        assertThat(intercept(new TrackedResponse(bytes(LIMIT))).getBody().readAllBytes())
                .hasSize(LIMIT);
    }

    @Test
    void should_fail_while_reading_when_the_body_exceeds_the_limit_without_content_length() throws IOException {
        ClientHttpResponse limited = intercept(new TrackedResponse(bytes(LIMIT + 1)));

        assertThatThrownBy(() -> limited.getBody().readAllBytes()).isInstanceOf(ResponseTooLargeException.class);
    }

    @Test
    void should_fail_while_reading_when_content_length_understates_the_body() throws IOException {
        TrackedResponse response = new TrackedResponse(bytes(LIMIT * 4));
        response.getHeaders().setContentLength(2);

        ClientHttpResponse limited = intercept(response);

        assertThatThrownBy(() -> limited.getBody().readAllBytes()).isInstanceOf(ResponseTooLargeException.class);
    }

    @Test
    void should_enforce_the_limit_on_single_byte_reads() throws IOException {
        InputStream body = intercept(new TrackedResponse(bytes(LIMIT + 1))).getBody();
        for (int i = 0; i < LIMIT; i++) {
            assertThat(body.read()).isNotNegative();
        }

        assertThatThrownBy(body::read).isInstanceOf(ResponseTooLargeException.class);
    }

    @Test
    void should_report_end_of_stream_normally_below_the_limit() throws IOException {
        InputStream body = intercept(new TrackedResponse(bytes(3))).getBody();

        assertThat(body.readAllBytes()).hasSize(3);
        assertThat(body.read()).isEqualTo(-1);
        assertThat(body.read(new byte[4], 0, 4)).isEqualTo(-1);
    }

    @Test
    void should_not_read_past_the_limit_into_memory() throws IOException {
        // A body far larger than the limit: at most limit + 1 bytes are ever pulled from upstream.
        CountingStream source = new CountingStream(1_000_000);
        TrackedResponse response = new TrackedResponse(source);

        InputStream body = intercept(response).getBody();

        assertThatThrownBy(body::readAllBytes).isInstanceOf(ResponseTooLargeException.class);
        assertThat(source.served).isLessThanOrEqualTo(LIMIT + 1L);
    }

    @Test
    void should_keep_status_and_headers_of_the_delegate() throws IOException {
        TrackedResponse response = new TrackedResponse(bytes(1));
        response.getHeaders().add("X-Test", "yes");

        ClientHttpResponse limited = intercept(response);

        assertThat(limited.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(limited.getStatusText()).isEqualTo("OK");
        assertThat(limited.getHeaders().getFirst("X-Test")).isEqualTo("yes");
        limited.close();
        assertThat(response.closed).isTrue();
    }

    @Test
    void should_not_leak_the_upstream_size_or_url_in_the_message() {
        TrackedResponse response = new TrackedResponse(bytes(LIMIT + 1));
        response.getHeaders().setContentLength(123_456_789L);

        assertThatThrownBy(() -> intercept(response))
                .hasMessage("PokeAPI response exceeds the configured size limit")
                .message()
                .doesNotContain("123456789")
                .doesNotContain("pokeapi.test");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void should_reject_a_non_positive_limit(long limit) {
        assertThatThrownBy(() -> new ResponseSizeLimitInterceptor(limit)).isInstanceOf(IllegalArgumentException.class);
    }

    private ClientHttpResponse intercept(ClientHttpResponse response) throws IOException {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("https://pokeapi.test/x"));
        return interceptor.intercept(request, new byte[0], (req, body) -> response);
    }

    private static byte[] bytes(int length) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) 'a');
        return bytes;
    }

    /** MockClientHttpResponse that remembers whether it was closed. */
    private static final class TrackedResponse extends MockClientHttpResponse {

        private boolean closed;

        TrackedResponse(byte[] body) {
            super(body, HttpStatus.OK);
        }

        TrackedResponse(InputStream body) {
            super(body, HttpStatus.OK);
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }
    }

    /** An endless-looking stream that counts how many bytes were pulled from it. */
    private static final class CountingStream extends InputStream {

        private final long size;
        private long served;

        CountingStream(long size) {
            this.size = size;
        }

        @Override
        public int read() {
            if (served >= size) {
                return -1;
            }
            served++;
            return 'a';
        }
    }
}
