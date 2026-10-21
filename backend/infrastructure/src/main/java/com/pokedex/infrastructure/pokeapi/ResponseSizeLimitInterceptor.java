package com.pokedex.infrastructure.pokeapi;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Caps the size of PokeAPI responses (security.md §4.5, threat T8):
 *
 * <ol>
 *   <li>A {@code Content-Length} over the limit is rejected before the body is touched, and the response is
 *       closed.
 *   <li>The body is then wrapped in a stream that fails as soon as more than the limit has been read. Neither a
 *       missing nor an understated header gets past it, and at most {@code limit + 1} bytes are ever pulled from
 *       the connection. Nothing is buffered here.
 * </ol>
 *
 * Both paths throw {@link ResponseTooLargeException}.
 */
public final class ResponseSizeLimitInterceptor implements ClientHttpRequestInterceptor {

    private final long maxBytes;

    public ResponseSizeLimitInterceptor(long maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        this.maxBytes = maxBytes;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        if (response.getHeaders().getContentLength() > maxBytes) {
            response.close();
            throw new ResponseTooLargeException();
        }
        return new LimitedResponse(response, maxBytes);
    }

    private static final class LimitedResponse implements ClientHttpResponse {

        private final ClientHttpResponse delegate;
        private final long maxBytes;
        private InputStream body;

        LimitedResponse(ClientHttpResponse delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

        @Override
        public InputStream getBody() throws IOException {
            if (body == null) {
                body = new LimitedInputStream(delegate.getBody(), maxBytes);
            }
            return body;
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    private static final class LimitedInputStream extends FilterInputStream {

        private final long maxBytes;
        private long read;

        LimitedInputStream(InputStream in, long maxBytes) {
            super(in);
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int next = super.read();
            if (next >= 0) {
                count(1);
            }
            return next;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            // Never ask for more than one byte past the limit, so an oversized body is not pulled into memory.
            long allowed = maxBytes - read + 1;
            int n = super.read(buffer, offset, (int) Math.min(length, allowed));
            if (n > 0) {
                count(n);
            }
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = super.skip(Math.min(n, maxBytes - read + 1));
            count(skipped);
            return skipped;
        }

        private void count(long bytes) throws ResponseTooLargeException {
            read += bytes;
            if (read > maxBytes) {
                throw new ResponseTooLargeException();
            }
        }
    }
}
