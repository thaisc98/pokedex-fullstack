package com.pokedex.domain.identity;

import java.util.Set;

/** Access level (security.md §3.1). An {@code ADMIN} can do everything a {@code USER} can. */
public enum Role {
    USER,
    ADMIN;

    /** This role plus every role it includes. */
    public Set<Role> impliedRoles() {
        return switch (this) {
            case USER -> Set.of(USER);
            case ADMIN -> Set.of(USER, ADMIN);
        };
    }
}
