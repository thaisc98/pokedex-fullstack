package com.pokedex.domain.shared;

/** UC-08: no usable principal, e.g. the token's user no longer exists (401 {@code /problems/unauthorized}). */
public final class AuthenticationRequiredException extends DomainException {

    private static final long serialVersionUID = 1L;

    public AuthenticationRequiredException() {
        super("authentication required");
    }
}
