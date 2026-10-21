package com.pokedex.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@code Role} (security.md §3.1): exactly {@code USER} and {@code ADMIN}; an {@code ADMIN} can do everything a
 * {@code USER} can. Design choice: the hierarchy lives here as {@code impliedRoles()} so the token issuer and
 * the delete use case (defence in depth, §3.2) read it from one place.
 */
class RoleTest {

    @Test
    void should_have_exactly_user_and_admin_matching_the_contract() {
        assertThat(Role.values()).extracting(Role::name).containsExactly("USER", "ADMIN");
    }

    @Test
    void should_grant_a_user_only_the_user_role() {
        assertThat(Role.USER.impliedRoles()).containsExactly(Role.USER);
    }

    @Test
    void should_grant_an_admin_both_roles() {
        assertThat(Role.ADMIN.impliedRoles()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
    }
}
