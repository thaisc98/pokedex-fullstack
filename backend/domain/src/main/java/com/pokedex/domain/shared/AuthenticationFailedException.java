package com.pokedex.domain.shared;

/**
 * UC-07: the credentials were not accepted (401 {@code /problems/invalid-credentials}). One generic message for
 * every cause, so nothing reveals which emails exist (security.md §4.1).
 */
public final class AuthenticationFailedException extends DomainException {

    private static final long serialVersionUID = 1L;

    public AuthenticationFailedException() {
        super("invalid email or password");
    }
}
