package com.pokedex.domain.shared;

import java.util.Objects;

/**
 * A value object rule was broken. Carries the domain field name and a message that is safe to show to
 * clients: the message never contains the rejected value.
 */
public final class InvalidValueException extends DomainException {

    private static final long serialVersionUID = 1L;

    private final String field;

    public InvalidValueException(String field, String message) {
        super(Objects.requireNonNull(message, "message"));
        this.field = Objects.requireNonNull(field, "field");
    }

    public String field() {
        return field;
    }
}
