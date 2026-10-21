package com.pokedex.domain.shared;

/**
 * The external catalog source could not produce a complete answer: it is down, erroring, timing out, or
 * returning data that fails the domain rules. Never results in a partial answer (architecture.md §1.5).
 */
public final class UpstreamUnavailableException extends DomainException {

    private static final long serialVersionUID = 1L;

    public UpstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
