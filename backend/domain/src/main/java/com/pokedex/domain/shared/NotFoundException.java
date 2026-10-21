package com.pokedex.domain.shared;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The requested resource does not exist → 404 {@code /problems/not-found}. The message names only the kind of
 * resource, never the id or name that was asked for.
 */
public final class NotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    private final String resource;

    public NotFoundException(String resource) {
        this(resource, null);
    }

    public NotFoundException(String resource, @Nullable Throwable cause) {
        super(Objects.requireNonNull(resource, "resource") + " not found", cause);
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}
