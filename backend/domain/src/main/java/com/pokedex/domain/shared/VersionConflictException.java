package com.pokedex.domain.shared;

/**
 * BR-08: the caller's {@code version} is not the stored one, because someone else changed the record first.
 * Maps to 409 {@code /problems/version-conflict}. The message is fixed and never echoes input.
 */
public final class VersionConflictException extends DomainException {

    private static final long serialVersionUID = 1L;

    public VersionConflictException() {
        super("the resource was modified by someone else; reload and retry");
    }
}
