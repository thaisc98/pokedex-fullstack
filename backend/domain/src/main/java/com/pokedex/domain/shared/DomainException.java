package com.pokedex.domain.shared;

/**
 * Root of every business-rule failure. Sealed so the presentation layer's error handler can switch over
 * it exhaustively and map each subtype to one RFC 9457 problem type (architecture.md §2.6).
 *
 * <p>An abstract class rather than a sealed interface: only {@link Throwable} subtypes can be thrown and
 * caught.
 */
public abstract sealed class DomainException extends RuntimeException
        permits AuthenticationFailedException,
                AuthenticationRequiredException,
                EmailAlreadyRegisteredException,
                InvalidValueException,
                UpstreamUnavailableException {

    private static final long serialVersionUID = 1L;

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
