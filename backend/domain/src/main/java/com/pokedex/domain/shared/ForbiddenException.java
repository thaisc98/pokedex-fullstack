package com.pokedex.domain.shared;

/**
 * The authenticated actor may not perform this operation (BR-10), checked in the use case as defence in
 * depth behind the security filter chain. Maps to 403 {@code /problems/forbidden}.
 */
public final class ForbiddenException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException() {
        super("not allowed to perform this operation");
    }
}
