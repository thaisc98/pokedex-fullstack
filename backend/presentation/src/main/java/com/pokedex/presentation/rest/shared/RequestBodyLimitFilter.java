package com.pokedex.presentation.rest.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * security.md §4.3 / T9: request bodies are capped at 16 KB. A declared {@code Content-Length} over the cap is refused
 * without reading; a body of unknown length (chunked) is read up to the cap + 1 byte, then refused or replayed. The
 * refusal is a 413 {@code /problems/payload-too-large} ProblemDetail. Runs before the security chain, so an oversized
 * body is never buffered past the cap.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestBodyLimitFilter extends OncePerRequestFilter {

    public static final int MAX_BODY_BYTES = 16 * 1024;

    static final String PROBLEM = "{\"type\":\"/problems/payload-too-large\",\"title\":\"Payload too large\","
            + "\"status\":413,\"detail\":\"The request body must be at most 16 KB.\"}";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long declared = request.getContentLengthLong();
        if (declared > MAX_BODY_BYTES) {
            reject(response);
            return;
        }
        if (declared >= 0 || !hasBody(request)) {
            chain.doFilter(request, response);
            return;
        }
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            reject(response);
            return;
        }
        chain.doFilter(new ReplayedBodyRequest(request, body), response);
    }

    private static boolean hasBody(HttpServletRequest request) {
        return request.getHeader("Transfer-Encoding") != null;
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Connection", "close");
        response.getWriter().write(PROBLEM);
    }

    /** The already-read body, served again to the rest of the chain. */
    private static final class ReplayedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        ReplayedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        @Override
        public ServletInputStream getInputStream() {
            InputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() throws IOException {
                    return in.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    return in.read(b, off, len);
                }

                @Override
                public boolean isFinished() {
                    return false;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("synchronous only");
                }
            };
        }
    }
}
