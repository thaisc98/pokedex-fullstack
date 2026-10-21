package com.pokedex.infrastructure.pokeapi;

import java.io.IOException;

/**
 * A PokeAPI response was larger than {@code pokeapi.max-response-bytes} (security.md §4.5). An {@link
 * IOException} so it can be thrown from the HTTP interceptor and from the body stream. Never retried. The
 * message is fixed: it carries neither the upstream size nor the URL.
 */
public final class ResponseTooLargeException extends IOException {

    private static final long serialVersionUID = 1L;

    public ResponseTooLargeException() {
        super("PokeAPI response exceeds the configured size limit");
    }
}
