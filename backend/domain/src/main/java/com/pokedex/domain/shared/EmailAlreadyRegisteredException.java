package com.pokedex.domain.shared;

/** UC-06: the email belongs to another account (409 {@code /problems/email-taken}). Never carries the email. */
public final class EmailAlreadyRegisteredException extends DomainException {

    private static final long serialVersionUID = 1L;

    public EmailAlreadyRegisteredException() {
        super("email is already registered");
    }
}
